package posta;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/15/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
@CsvRecord(separator = ",", crlf = ",\n")
public class ModelClass {

    @DataField(pos = 1)
    private int id;

    @DataField(pos = 2)
    private int status;

    @DataField(pos = 3)
    private String comment;

    @DataField(pos = 4)
    private String amount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
