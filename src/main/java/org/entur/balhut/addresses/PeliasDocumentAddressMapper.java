package org.entur.balhut.addresses;

import org.entur.balhut.addresses.coordinates.GeometryTransformer;
import org.entur.balhut.addresses.kartverket.KartverketAddress;
import org.entur.balhut.addresses.kartverket.KartverketCoordinateSystemMapper;
import org.entur.geocoder.model.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PeliasDocumentAddressMapper {

  private static final String DEFAULT_SOURCE = "KVE";
  private static final String DEFAULT_LAYER = "Address";

  // Use unique source for addresses to allow for filtering them out from pelias autocomplete
  private final long popularity;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final GeometryFactory factory = new GeometryFactory();

  public PeliasDocumentAddressMapper(
    @Value("${pelias.address.boost:2}") long popularity
  ) {
    this.popularity = popularity;
  }

  public PeliasDocument toPeliasDocument(KartverketAddress address) {
    PeliasDocument document = new PeliasDocument(
      new PeliasId(DEFAULT_SOURCE, DEFAULT_LAYER, address.getAddresseId())
    );
    document.setAddressParts(toAddressParts(address));

    GeoPoint centerPoint = toCenterPoint(address);
    document.setCenterPoint(centerPoint);

    setParent(document, address);

    document.setDefaultName(toName(address));
    document.addCategory(address.getType());
    document.setPopularity(popularity);
    return document;
  }

  private String toName(KartverketAddress address) {
    return (
      address.getNr() + address.getBokstav() + " " + address.getAddressenavn()
    ).trim();
  }

  private GeoPoint toCenterPoint(KartverketAddress address) {
    if (address.getNord() == null || address.getOst() == null) {
      return null;
    }
    String utmZone = KartverketCoordinateSystemMapper.toUTMZone(
      address.getKoordinatsystemKode()
    );
    if (utmZone == null) {
      logger.info(
        "Ignoring center point for address with non-utm coordinate system: " +
        address.getKoordinatsystemKode()
      );
      return null;
    }
    Point p = factory.createPoint(
      new Coordinate(address.getOst(), address.getNord())
    );
    try {
      Point conv = GeometryTransformer.fromUTM(p, utmZone);
      return new GeoPoint(conv.getY(), conv.getX());
    } catch (Exception e) {
      logger.info(
        "Ignoring center point for address " +
        "(" +
        address.getAddresseId() +
        ") " +
        "where geometry transformation failed: " +
        address.getKoordinatsystemKode()
      );
    }

    return null;
  }

  private static void setParent(
    PeliasDocument document,
    KartverketAddress address
  ) {
    document
      .getParents()
      .addOrReplaceParent(
        ParentType.LOCALITY,
        createPeliasIdForParent(address.getKommunenr()),
        capitalize(address.getKommunenavn())
      );

    document
      .getParents()
      .addOrReplaceParent(
        ParentType.POSTAL_CODE,
        createPeliasIdForParent(address.getPostnrn()),
        capitalize(address.getPostnummeromrade())
      );

    document
      .getParents()
      .addOrReplaceParent(
        ParentType.BOROUGH,
        createPeliasIdForParent(address.getGrunnkretsnr()),
        capitalize(address.getGrunnkretsnavn())
      );
  }

  private static PeliasId createPeliasIdForParent(String id) {
    return new PeliasId("KVE", "TopographicPlace", id);
  }

  private static String capitalize(String string) {
    return string == null ? null : StringUtils.capitalize(string.toLowerCase());
  }

  private AddressParts toAddressParts(KartverketAddress address) {
    return new AddressParts(
      address.getAddressenavn(),
      address.getNr() + address.getBokstav(),
      address.getPostnrn()
    );
  }
}
