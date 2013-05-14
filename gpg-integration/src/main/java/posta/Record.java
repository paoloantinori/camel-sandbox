package posta; 

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/16/13
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
// maye use System.getProperty("line.separator") instead of hardcoded \n
@CsvRecord(separator = ",", crlf = ",\n")
public class Record {

    @DataField(pos = 1)
    private String disbursementCode;

    @DataField(pos = 2)
    private String certificateNumber;

    @DataField(pos = 3)
    private String certificateValue;

    @DataField(pos = 4)
    private String certificateEffectiveDate;

    @DataField(pos = 5)
    private String certificateExpirationDate;

    @DataField(pos = 6)
    private String disbursementModeUnitId;

    @DataField(pos = 7)
    private String pin;

    public String getDisbursementCode() {
        return disbursementCode;
    }

    public void setDisbursementCode(String disbursementCode) {
        this.disbursementCode = disbursementCode;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getCertificateValue() {
        return certificateValue;
    }

    public void setCertificateValue(String certificateValue) {
        this.certificateValue = certificateValue;
    }

    public String getCertificateEffectiveDate() {
        return certificateEffectiveDate;
    }

    public void setCertificateEffectiveDate(String certificateEffectiveDate) {
        this.certificateEffectiveDate = certificateEffectiveDate;
    }

    public String getCertificateExpirationDate() {
        return certificateExpirationDate;
    }

    public void setCertificateExpirationDate(String certificateExpirationDate) {
        this.certificateExpirationDate = certificateExpirationDate;
    }

    public String getDisbursementModeUnitId() {
        return disbursementModeUnitId;
    }

    public void setDisbursementModeUnitId(String disbursementModeUnitId) {
        this.disbursementModeUnitId = disbursementModeUnitId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
