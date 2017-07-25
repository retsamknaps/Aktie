package aktie.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.json.JSONObject;

@Entity
public class CommunityMember
{

    public static int DONE = 0;
    public static int UPDATE = 1;
    public static int REQUESTED = 2;

    @Id
    private String id;
    private String communityId;
    private String memberId;

    private long lastSubscriptionNumber;
    private long lastPostNumber;
    private long lastFileNumber;

    private long nextClosestSubscriptionNumber;
    private long nextClosestPostNumber;
    private long nextClosestFileNumber;

    private int numClosestSubscriptionNumber;
    private int numClosestPostNumber;
    private int numClosestFileNumber;

    private long lastSubscriptionUpdate;
    private int subscriptionStatus;
    private int subscriptionUpdatePriority;
    private String lastSubscriptionUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int subscriptionUpdateCycle;

    private long lastPostUpdate;
    private int postStatus;
    private int postUpdatePriority;
    private String lastPostUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int postUpdateCycle;

    private long lastFileUpdate;
    private int fileStatus;
    private int fileUpdatePriority;
    private String lastFileUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int fileUpdateCycle;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long lastGlobalSequence;

    public CommunityMember()
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

    //    public long getLastSubscriptionNumber()
    //    {
    //        return lastSubscriptionNumber;
    //    }

    //
    //    public void setLastSubscriptionNumber ( long lastSubscriptionNumber )
    //    {
    //        this.lastSubscriptionNumber = lastSubscriptionNumber;
    //    }

    public long getLastPostNumber()
    {
        return lastPostNumber;
    }

    public void setLastPostNumber ( long lastPostNumber )
    {
        this.lastPostNumber = lastPostNumber;
    }

    public long getLastFileNumber()
    {
        return lastFileNumber;
    }

    public void setLastFileNumber ( long lastFileNumber )
    {
        this.lastFileNumber = lastFileNumber;
    }

    //    public long getLastSubscriptionUpdate()
    //    {
    //        return lastSubscriptionUpdate;
    //    }

    //
    //    public void setLastSubscriptionUpdate ( long lastSubscriptionUpdate )
    //    {
    //        this.lastSubscriptionUpdate = lastSubscriptionUpdate;
    //    }

    //
    //    public int getSubscriptionStatus()
    //    {
    //        return subscriptionStatus;
    //    }

    //
    //    public void setSubscriptionStatus ( int subscriptionStatus )
    //    {
    //        this.subscriptionStatus = subscriptionStatus;
    //    }

    public long getLastPostUpdate()
    {
        return lastPostUpdate;
    }

    public void setLastPostUpdate ( long lastPostUpdate )
    {
        this.lastPostUpdate = lastPostUpdate;
    }

    public int getPostStatus()
    {
        return postStatus;
    }

    public void setPostStatus ( int postStatus )
    {
        this.postStatus = postStatus;
    }

    public long getLastFileUpdate()
    {
        return lastFileUpdate;
    }

    public void setLastFileUpdate ( long lastFileUpdate )
    {
        this.lastFileUpdate = lastFileUpdate;
    }

    public int getFileStatus()
    {
        return fileStatus;
    }

    public void setFileStatus ( int fileStatus )
    {
        this.fileStatus = fileStatus;
    }

    //    public int getSubscriptionUpdatePriority()
    //    {
    //        return subscriptionUpdatePriority;
    //    }

    //
    //    public void setSubscriptionUpdatePriority ( int subscriptionUpdatePriority )
    //    {
    //        this.subscriptionUpdatePriority = subscriptionUpdatePriority;
    //    }

    public int getPostUpdatePriority()
    {
        return postUpdatePriority;
    }

    public void setPostUpdatePriority ( int postUpdatePriority )
    {
        this.postUpdatePriority = postUpdatePriority;
    }

    public int getFileUpdatePriority()
    {
        return fileUpdatePriority;
    }

    public void setFileUpdatePriority ( int fileUpdatePriority )
    {
        this.fileUpdatePriority = fileUpdatePriority;
    }

    //    public long getNextClosestSubscriptionNumber()
    //    {
    //        return nextClosestSubscriptionNumber;
    //    }

    //
    //    public void setNextClosestSubscriptionNumber ( long nextClosestSubscriptionNumber )
    //    {
    //        this.nextClosestSubscriptionNumber = nextClosestSubscriptionNumber;
    //    }

    public long getNextClosestPostNumber()
    {
        return nextClosestPostNumber;
    }

    public void setNextClosestPostNumber ( long nextClosestPostNumber )
    {
        this.nextClosestPostNumber = nextClosestPostNumber;
    }

    public long getNextClosestFileNumber()
    {
        return nextClosestFileNumber;
    }

    public void setNextClosestFileNumber ( long nextClosestFileNumber )
    {
        this.nextClosestFileNumber = nextClosestFileNumber;
    }

    //    public int getNumClosestSubscriptionNumber()
    //    {
    //        return numClosestSubscriptionNumber;
    //    }

    //
    //    public void setNumClosestSubscriptionNumber ( int numClosestSubscriptionNumber )
    //    {
    //        this.numClosestSubscriptionNumber = numClosestSubscriptionNumber;
    //    }

    public int getNumClosestPostNumber()
    {
        return numClosestPostNumber;
    }

    public void setNumClosestPostNumber ( int numClosestPostNumber )
    {
        this.numClosestPostNumber = numClosestPostNumber;
    }

    public int getNumClosestFileNumber()
    {
        return numClosestFileNumber;
    }

    public void setNumClosestFileNumber ( int numClosestFileNumber )
    {
        this.numClosestFileNumber = numClosestFileNumber;
    }

    //    public String getLastSubscriptionUpdateFrom()
    //    {
    //        return lastSubscriptionUpdateFrom;
    //    }

    //
    //    public void setLastSubscriptionUpdateFrom ( String lastSubscriptionUpdateFrom )
    //    {
    //        this.lastSubscriptionUpdateFrom = lastSubscriptionUpdateFrom;
    //    }

    public String getLastPostUpdateFrom()
    {
        return lastPostUpdateFrom;
    }

    public void setLastPostUpdateFrom ( String lastPostUpdateFrom )
    {
        this.lastPostUpdateFrom = lastPostUpdateFrom;
    }

    public String getLastFileUpdateFrom()
    {
        return lastFileUpdateFrom;
    }

    public void setLastFileUpdateFrom ( String lastFileUpdateFrom )
    {
        this.lastFileUpdateFrom = lastFileUpdateFrom;
    }

    //    public int getSubscriptionUpdateCycle()
    //    {
    //        return subscriptionUpdateCycle;
    //    }

    //
    //    public void setSubscriptionUpdateCycle ( int subscriptionUpdateCycle )
    //    {
    //        this.subscriptionUpdateCycle = subscriptionUpdateCycle;
    //    }

    public int getPostUpdateCycle()
    {
        return postUpdateCycle;
    }

    public void setPostUpdateCycle ( int postUpdateCycle )
    {
        this.postUpdateCycle = postUpdateCycle;
    }

    public int getFileUpdateCycle()
    {
        return fileUpdateCycle;
    }

    public void setFileUpdateCycle ( int fileUpdateCycle )
    {
        this.fileUpdateCycle = fileUpdateCycle;
    }

    public long getLastGlobalSequence()
    {
        return lastGlobalSequence;
    }

    public void setLastGlobalSequence ( long lastGlobalSequence )
    {
        this.lastGlobalSequence = lastGlobalSequence;
    }

    public String toString()
    {
        JSONObject o = new JSONObject ( this );
        return o.toString ( 4 );
    }

}
