package aktie.gui.subtree;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.json.JSONObject;

@Entity
public class SubTreeEntity implements Comparable<Object>
{

    public static int IDENTITY_TYPE = 0;
    public static int FOLDER_TYPE = 1;
    public static int PUBCOMMUNITY_TYPE = 2;
    public static int PRVCOMMUNITY_TYPE = 3;
    public static int PRVMESSAGE_TYPE = 4;

    @Id
    @GeneratedValue
    private long id;
    private int type;
    private long sortOrder;
    private String text;
    private long parent;
    private boolean blue;
    private boolean hidden;
    private boolean collapsed;
    private String identity;
    private String refId;
    private boolean connected;

    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int treeId;

    public SubTreeEntity()
    {
    }

    public long getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder ( long sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    public String getText()
    {
        return text;
    }

    public void setText ( String text )
    {
        this.text = text;
    }

    public long getParent()
    {
        return parent;
    }

    public void setParent ( long parent )
    {
        this.parent = parent;
    }

    public long getId()
    {
        return id;
    }

    public void setId ( long id )
    {
        this.id = id;
    }

    public boolean isBlue()
    {
        return blue;
    }

    public void setBlue ( boolean blue )
    {
        this.blue = blue;
    }

    public int getType()
    {
        return type;
    }

    public void setType ( int type )
    {
        this.type = type;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden ( boolean hidden )
    {
        this.hidden = hidden;
    }

    @Override
    public int compareTo ( Object o )
    {
        if ( o != null && o instanceof SubTreeEntity )
        {
            SubTreeEntity s = ( SubTreeEntity ) o;

            if ( s.sortOrder == sortOrder )
            {
                if ( s.type == type )
                {
                    if ( text != null && s.text != null )
                    {
                        return text.compareTo ( s.text );
                    }

                }

                return type - s.type;
            }

            long d = sortOrder - s.sortOrder;

            if ( d == 0 ) { return 0; }

            if ( d < 0 ) { return -1; }

            return 1;
        }

        return 0;
    }

    @Override
    public int hashCode()
    {
        return ( int ) id;
    }

    @Override
    public boolean equals ( Object o )
    {
        if ( o == null ) { return false; }

        if ( ! ( o instanceof SubTreeEntity ) ) { return false; }

        SubTreeEntity s = ( SubTreeEntity ) o;
        return id == s.getId();
    }

    public String toString()
    {
        JSONObject o = new JSONObject ( this );
        return o.toString ( 4 );
    }

    public String getIdentity()
    {
        return identity;
    }

    public void setIdentity ( String identity )
    {
        this.identity = identity;
    }

    public boolean isCollapsed()
    {
        return collapsed;
    }

    public void setCollapsed ( boolean collapsed )
    {
        this.collapsed = collapsed;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public void setConnected ( boolean connected )
    {
        this.connected = connected;
    }

    public String getRefId()
    {
        return refId;
    }

    public void setRefId ( String refId )
    {
        this.refId = refId;
    }

    public int getTreeId()
    {
        return treeId;
    }

    public void setTreeId ( int treeId )
    {
        this.treeId = treeId;
    }

}
