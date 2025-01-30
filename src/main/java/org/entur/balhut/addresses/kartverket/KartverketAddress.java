package org.entur.balhut.addresses.kartverket;

import com.opencsv.bean.CsvBindByPosition;
import org.apache.commons.lang3.StringUtils;

public class KartverketAddress {

  @CsvBindByPosition(position = 0)
  //    @CsvBindByName(column = "lokalid")
  private String addresseId;

  @CsvBindByPosition(position = 1)
  //    @CsvBindByName(column = "kommunenummer")
  private String kommunenr;

  @CsvBindByPosition(position = 2)
  //    @CsvBindByName(column = "kommunenavn")
  private String kommunenavn;

  @CsvBindByPosition(position = 3)
  //    @CsvBindByName(column = "adressetype")
  private String type;

  @CsvBindByPosition(position = 7)
  //    @CsvBindByName(column = "adressenavn")
  private String addressenavn;

  @CsvBindByPosition(position = 8)
  //    @CsvBindByName(column = "nummer")
  private String nr;

  @CsvBindByPosition(position = 9)
  //    @CsvBindByName(column = "bokstav")
  private String bokstav;

  @CsvBindByPosition(position = 16)
  //    @CsvBindByName(column = "EPSG-kode")
  private String koordinatsystemKode;

  @CsvBindByPosition(position = 17)
  //    @CsvBindByName(column = "Nord")
  private Double nord;

  @CsvBindByPosition(position = 18)
  //    @CsvBindByName(column = "Øst")
  private Double ost;

  @CsvBindByPosition(position = 19)
  //    @CsvBindByName(column = "postnummer")
  private String postnrn;

  @CsvBindByPosition(position = 20)
  //    @CsvBindByName(column = "poststed")
  private String postnummeromrade;

  @CsvBindByPosition(position = 21)
  //    @CsvBindByName(column = "grunnkretsnummer")
  private String grunnkretsnr;

  @CsvBindByPosition(position = 22)
  //    @CsvBindByName(column = "grunnkretsnavn")
  private String grunnkretsnavn;

  public String getAddresseId() {
    return addresseId;
  }

  public void setAddresseId(String addresseId) {
    this.addresseId = addresseId;
  }

  public String getKommunenr() {
    return kommunenr != null ? StringUtils.leftPad(kommunenr, 4, "0") : null;
  }

  public void setKommunenr(String kommunenr) {
    this.kommunenr = kommunenr;
  }

  public String getKommunenavn() {
    return kommunenavn;
  }

  public void setKommunenavn(String kommunenavn) {
    this.kommunenavn = kommunenavn;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAddressenavn() {
    return addressenavn;
  }

  public void setAddressenavn(String addressenavn) {
    this.addressenavn = addressenavn;
  }

  public String getNr() {
    return nr;
  }

  public void setNr(String nr) {
    this.nr = nr;
  }

  public String getBokstav() {
    return bokstav;
  }

  public void setBokstav(String bokstav) {
    this.bokstav = bokstav;
  }

  public String getKoordinatsystemKode() {
    return koordinatsystemKode;
  }

  public void setKoordinatsystemKode(String koordinatsystemKode) {
    this.koordinatsystemKode = koordinatsystemKode;
  }

  public Double getNord() {
    return nord;
  }

  public void setNord(Double nord) {
    this.nord = nord;
  }

  public Double getOst() {
    return ost;
  }

  public void setOst(Double ost) {
    this.ost = ost;
  }

  public String getPostnrn() {
    return postnrn;
  }

  public void setPostnrn(String postnrn) {
    this.postnrn = postnrn;
  }

  public String getPostnummeromrade() {
    return postnummeromrade;
  }

  public void setPostnummeromrade(String postnummeromrade) {
    this.postnummeromrade = postnummeromrade;
  }

  public String getGrunnkretsnr() {
    return grunnkretsnr;
  }

  public void setGrunnkretsnr(String grunnkretsnr) {
    this.grunnkretsnr = grunnkretsnr;
  }

  public String getGrunnkretsnavn() {
    return grunnkretsnavn;
  }

  public void setGrunnkretsnavn(String grunnkretsnavn) {
    this.grunnkretsnavn = grunnkretsnavn;
  }
}
