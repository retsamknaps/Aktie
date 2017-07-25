package aktie.data;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.json.JSONObject;

@Entity
public class CommunityMyMember
{

    @Id
    private String id;
    private String communityId;
    private String memberId;

    private long lastDecode;
    private byte key[];

    public CommunityMyMember()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId ( String id )
    {
        this.id = id;
    }

    public String getCommunityId()
    {
        return communityId;
    }

    public void setCommunityId ( String communityId )
    {
        this.communityId = communityId;
    }

    public String getMemberId()
    {
        return memberId;
    }

    public void setMemberId ( String memberId )
    {
        this.memberId = memberId;
    }

    public long getLastDecode()
    {
        return lastDecode;
    }

    public void setLastDecode ( long lastDecode )
    {
        this.lastDecode = lastDecode;
    }

    public byte[] getKey()
    {
        return key;
    }

    public void setKey ( byte[] key )
    {
        this.key = key;
    }

    public String toString()
    {
        JSONObject o = new JSONObject ( this );
        return o.toString ( 4 );
    }


}
