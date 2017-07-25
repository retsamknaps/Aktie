package aktie.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.json.JSONObject;

/**
    Request a file for download

*/
@Entity
public class RequestFile
{

    public static final int INIT = 0;
    public static final int REQUEST_FRAG_LIST = 1;
    public static final int REQUEST_FRAG_LIST_SNT = 2;
    public static final int REQUEST_FRAG = 3;
    public static final int COMPLETE = 4;

    @Id
    @GeneratedValue
    private long id;
    private String localFile;
    private String requestId;
    private String communityId;
    private String wholeDigest;
    private String fragmentDigest;
    private int state;
    private int priority;
    private long lastRequest;
    private long fragsComplete;
    private long fragsTotal;
    private long fragSize;
    private long fileSize;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long requestedOn;
    private boolean isUpgrade;
    private String shareName;

    public RequestFile()
    {
    }

    public long getId()
    {
        return id;
    }

    public void setId ( long id )
    {
        this.id = id;
    }

    public String getLocalFile()
    {
        return localFile;
    }

    public void setLocalFile ( String localFile )
    {
        this.localFile = localFile;
    }

    public String getWholeDigest()
    {
        return wholeDigest;
    }

    public void setWholeDigest ( String wholeDigest )
    {
        this.wholeDigest = wholeDigest;
    }

    public String getFragmentDigest()
    {
        return fragmentDigest;
    }

    public void setFragmentDigest ( String fragmentDigest )
    {
        this.fragmentDigest = fragmentDigest;
    }

    public int getState()
    {
        return state;
    }

    public void setState ( int state )
    {
        this.state = state;
    }

    public String getStateText()
    {
        String text = "";

        switch ( state )
        {

        case INIT:
            text = "init";
            break;

        case REQUEST_FRAG:
            text = "download parts";
            break;

        case REQUEST_FRAG_LIST:
            text = "requesting list";
            break;

        case REQUEST_FRAG_LIST_SNT:
            text = "waiting on list";
            break;

        case COMPLETE:
            text = "complete";
            break;

        default:
            text = this.state + "?";
            break;

        }

        return text;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority ( int priority )
    {
        this.priority = priority;
    }

    public long getLastRequest()
    {
        return lastRequest;
    }

    public void setLastRequest ( long lastRequest )
    {
        this.lastRequest = lastRequest;
    }

    public long getFragsComplete()
    {
        return fragsComplete;
    }

    public void setFragsComplete ( long fragsComplete )
    {
        this.fragsComplete = fragsComplete;
    }

    public long getFragsTotal()
    {
        return fragsTotal;
    }

    public void setFragsTotal ( long fragsTotal )
    {
        this.fragsTotal = fragsTotal;
    }

    public String getCommunityId()
    {
        return communityId;
    }

    public void setCommunityId ( String communityId )
    {
        this.communityId = communityId;
    }

    public long getFragSize()
    {
        return fragSize;
    }

    public void setFragSize ( long fragSize )
    {
        this.fragSize = fragSize;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize ( long fileSize )
    {
        this.fileSize = fileSize;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId ( String requestId )
    {
        this.requestId = requestId;
    }

    public boolean isUpgrade()
    {
        return isUpgrade;
    }

    public void setUpgrade ( boolean isUpgrade )
    {
        this.isUpgrade = isUpgrade;
    }

    public long getRequestedOn()
    {
        return requestedOn;
    }

    public void setRequestedOn ( long requestedOn )
    {
        this.requestedOn = requestedOn;
    }

    public String getShareName()
    {
        return shareName;
    }

    public void setShareName ( String shareName )
    {
        this.shareName = shareName;
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

        if ( this == o ) { return true; }

        if ( ! ( o instanceof RequestFile ) ) { return false; }

        RequestFile rf = ( RequestFile ) o;
        return id == rf.getId();
    }

    public String toString()
    {
        JSONObject o = new JSONObject ( this );
        return o.toString ( 4 );
    }


}
