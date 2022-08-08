package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "aggregator_table")
public class Aggregator {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public int type;

    public String name;

    public String nick;


    public Aggregator(int type, String name, String nick) {
        this.type = type;
        this.name = name;
        this.nick = nick;
    }

    public void setId(long id) {
        this.id = id;
    }
}
