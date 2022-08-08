package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_table")
public class Category {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public int type;
    public long aggregatorId;
    public String name;
    public String nick;
    public boolean active;
    public long last_update;

    public Category(int type, long aggregatorId, String name, String nick, boolean active, long last_update) {
        this.type = type;
        this.aggregatorId = aggregatorId;
        this.name = name;
        this.nick = nick;
        this.active = active;
        this.last_update = last_update;
    }

    public void setId(long id) {
        this.id = id;
    }
}
