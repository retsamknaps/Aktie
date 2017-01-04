package aktie.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


/**
    Internal information about identity not shared with others

*/
@Entity
public class IdentityData
{

    //Max Objects for a global sequence value.
    public static long MAXGLOBALSEQUENCECOUNT = 200;

    //Make sure the sequence number is incremented at least once a day.
    public static long MAXGLOBALSEQUENCETIME = 24L * 60L * 60L * 1000L;

    public static int DONE = 0;
    public static int UPDATE = 1;
    public static int REQUESTED = 2;

    @Id
    private String id;
    private long firstSeen;

    private long lastCommunityNumber;
    private long lastTemplateNumber;
    private long lastMembershipNumber;

    private long nextClosestCommunityNumber;
    private long nextClosestTempalteNumber;
    private long nextClosestMembershipNumber;

    private int numClosestCommunityNumber;
    private int numClosestTemplateNumber;
    private int numClosestMembershipNumber;

    private long lastConnectionAttempt;
    private long lastSuccessfulConnection;

    private long totalNonFileReceived;
    private long totalReceived;
    private long totalSent;

    private long lastIdentityUpdate;
    private int identityStatus;
    private int identityUpdatePriority;

    private long lastCommunityUpdate;
    private int communityStatus;
    private int communityUpdatePriority;
    private String lastCommunityUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int communityUpdateCycle;

    private long lastMemberUpdate;
    private int memberStatus;
    private int memberUpdatePriority;
    private String lastMemberUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int memberUpdateCycle;

    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long lastSubUpdate;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long lastSubNumber;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long nextClosestSubNumber;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int numClosestSubNumber;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int subStatus;
    private String lastSubUpdateFrom;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int subUpdateCycle;
    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int subUpdatePriority;

    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long lastGlobalSequence;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long countForLastGlobalSequence;
    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long nextGlobalSequenceUpdateTime;


    @Column ( columnDefinition = "BIGINT(19) default 0" )
    private long lastDataTime;

    private boolean mine;

    public String getId()
    {
        return id;
    }

    public void setId ( String id )
    {
        this.id = id;
    }

    public long getFirstSeen()
    {
        return firstSeen;
    }

    public void setFirstSeen ( long firstSeen )
    {
        this.firstSeen = firstSeen;
    }

    public long getLastCommunityNumber()
    {
        return lastCommunityNumber;
    }

    public void setLastCommunityNumber ( long lastCommunityNumber )
    {
        this.lastCommunityNumber = lastCommunityNumber;
    }

    public long getLastTemplateNumber()
    {
        return lastTemplateNumber;
    }

    public void setLastTemplateNumber ( long lastTemplateNumber )
    {
        this.lastTemplateNumber = lastTemplateNumber;
    }

    public long getLastMembershipNumber()
    {
        return lastMembershipNumber;
    }

    public void setLastMembershipNumber ( long lastMembershipNumber )
    {
        this.lastMembershipNumber = lastMembershipNumber;
    }

    public long getLastConnectionAttempt()
    {
        return lastConnectionAttempt;
    }

    public void setLastConnectionAttempt ( long lastConnectionAttempt )
    {
        this.lastConnectionAttempt = lastConnectionAttempt;
    }

    public long getLastSuccessfulConnection()
    {
        return lastSuccessfulConnection;
    }

    public void setLastSuccessfulConnection ( long lastSuccessfulConnection )
    {
        this.lastSuccessfulConnection = lastSuccessfulConnection;
    }

    public long getTotalNonFileReceived()
    {
        return totalNonFileReceived;
    }

    public void setTotalNonFileReceived ( long totalNonFileReceived )
    {
        this.totalNonFileReceived = totalNonFileReceived;
    }

    public long getTotalReceived()
    {
        return totalReceived;
    }

    public void setTotalReceived ( long totalReceived )
    {
        this.totalReceived = totalReceived;
    }

    public long getTotalSent()
    {
        return totalSent;
    }

    public void setTotalSent ( long totalSent )
    {
        this.totalSent = totalSent;
    }

    public long getLastCommunityUpdate()
    {
        return lastCommunityUpdate;
    }

    public void setLastCommunityUpdate ( long lastCommunityUpdate )
    {
        this.lastCommunityUpdate = lastCommunityUpdate;
    }

    public int getCommunityStatus()
    {
        return communityStatus;
    }

    public void setCommunityStatus ( int communityStatus )
    {
        this.communityStatus = communityStatus;
    }

    public long getLastMemberUpdate()
    {
        return lastMemberUpdate;
    }

    public void setLastMemberUpdate ( long lastMemberUpdate )
    {
        this.lastMemberUpdate = lastMemberUpdate;
    }

    public int getMemberStatus()
    {
        return memberStatus;
    }

    public void setMemberStatus ( int memberStatus )
    {
        this.memberStatus = memberStatus;
    }

    public int getCommunityUpdatePriority()
    {
        return communityUpdatePriority;
    }

    public void setCommunityUpdatePriority ( int communityUpdatePriority )
    {
        this.communityUpdatePriority = communityUpdatePriority;
    }

    public int getMemberUpdatePriority()
    {
        return memberUpdatePriority;
    }

    public void setMemberUpdatePriority ( int memberUpdatePriority )
    {
        this.memberUpdatePriority = memberUpdatePriority;
    }

    public long getLastIdentityUpdate()
    {
        return lastIdentityUpdate;
    }

    public void setLastIdentityUpdate ( long lastIdentityUpdate )
    {
        this.lastIdentityUpdate = lastIdentityUpdate;
    }

    public int getIdentityStatus()
    {
        return identityStatus;
    }

    public void setIdentityStatus ( int identityStatus )
    {
        this.identityStatus = identityStatus;
    }

    public int getIdentityUpdatePriority()
    {
        return identityUpdatePriority;
    }

    public void setIdentityUpdatePriority ( int identityUpdatePriority )
    {
        this.identityUpdatePriority = identityUpdatePriority;
    }

    public boolean isMine()
    {
        return mine;
    }

    public void setMine ( boolean mine )
    {
        this.mine = mine;
    }

    public long getNextClosestCommunityNumber()
    {
        return nextClosestCommunityNumber;
    }

    public void setNextClosestCommunityNumber ( long nextClosestCommunityNumber )
    {
        this.nextClosestCommunityNumber = nextClosestCommunityNumber;
    }

    public long getNextClosestTempalteNumber()
    {
        return nextClosestTempalteNumber;
    }

    public void setNextClosestTempalteNumber ( long nextClosestTempalteNumber )
    {
        this.nextClosestTempalteNumber = nextClosestTempalteNumber;
    }

    public long getNextClosestMembershipNumber()
    {
        return nextClosestMembershipNumber;
    }

    public void setNextClosestMembershipNumber ( long nextClosestMembershipNumber )
    {
        this.nextClosestMembershipNumber = nextClosestMembershipNumber;
    }

    public int getNumClosestCommunityNumber()
    {
        return numClosestCommunityNumber;
    }

    public void setNumClosestCommunityNumber ( int numClosestCommunityNumber )
    {
        this.numClosestCommunityNumber = numClosestCommunityNumber;
    }

    public int getNumClosestTemplateNumber()
    {
        return numClosestTemplateNumber;
    }

    public void setNumClosestTemplateNumber ( int numClosestTemplateNumber )
    {
        this.numClosestTemplateNumber = numClosestTemplateNumber;
    }

    public int getNumClosestMembershipNumber()
    {
        return numClosestMembershipNumber;
    }

    public void setNumClosestMembershipNumber ( int numClosestMembershipNumber )
    {
        this.numClosestMembershipNumber = numClosestMembershipNumber;
    }

    public String getLastCommunityUpdateFrom()
    {
        return lastCommunityUpdateFrom;
    }

    public void setLastCommunityUpdateFrom ( String lastCommunityUpdateFrom )
    {
        this.lastCommunityUpdateFrom = lastCommunityUpdateFrom;
    }

    public int getCommunityUpdateCycle()
    {
        return communityUpdateCycle;
    }

    public void setCommunityUpdateCycle ( int communityUpdateCycle )
    {
        this.communityUpdateCycle = communityUpdateCycle;
    }

    public String getLastMemberUpdateFrom()
    {
        return lastMemberUpdateFrom;
    }

    public void setLastMemberUpdateFrom ( String lastMemberUpdateFrom )
    {
        this.lastMemberUpdateFrom = lastMemberUpdateFrom;
    }

    public int getMemberUpdateCycle()
    {
        return memberUpdateCycle;
    }

    public void setMemberUpdateCycle ( int memberUpdateCycle )
    {
        this.memberUpdateCycle = memberUpdateCycle;
    }

    public long getLastSubUpdate()
    {
        return lastSubUpdate;
    }

    public void setLastSubUpdate ( long lastSubUpdate )
    {
        this.lastSubUpdate = lastSubUpdate;
    }

    public long getLastSubNumber()
    {
        return lastSubNumber;
    }

    public void setLastSubNumber ( long lastSubNumber )
    {
        this.lastSubNumber = lastSubNumber;
    }

    public long getNextClosestSubNumber()
    {
        return nextClosestSubNumber;
    }

    public void setNextClosestSubNumber ( long nextClosestSubNumber )
    {
        this.nextClosestSubNumber = nextClosestSubNumber;
    }

    public int getNumClosestSubNumber()
    {
        return numClosestSubNumber;
    }

    public void setNumClosestSubNumber ( int numClosestSubNumber )
    {
        this.numClosestSubNumber = numClosestSubNumber;
    }

    public int getSubStatus()
    {
        return subStatus;
    }

    public void setSubStatus ( int subStatus )
    {
        this.subStatus = subStatus;
    }

    public String getLastSubUpdateFrom()
    {
        return lastSubUpdateFrom;
    }

    public void setLastSubUpdateFrom ( String lastSubUpdateFrom )
    {
        this.lastSubUpdateFrom = lastSubUpdateFrom;
    }

    public int getSubUpdateCycle()
    {
        return subUpdateCycle;
    }

    public void setSubUpdateCycle ( int subUpdateCycle )
    {
        this.subUpdateCycle = subUpdateCycle;
    }

    public int getSubUpdatePriority()
    {
        return subUpdatePriority;
    }

    public void setSubUpdatePriority ( int subUpdatePriority )
    {
        this.subUpdatePriority = subUpdatePriority;
    }

    public long getLastGlobalSequence()
    {
        return lastGlobalSequence;
    }

    public void setLastGlobalSequence ( long lastGlobalSequence )
    {
        this.lastGlobalSequence = lastGlobalSequence;
    }

    public long getCountForLastGlobalSequence()
    {
        return countForLastGlobalSequence;
    }

    public void setCountForLastGlobalSequence ( long countForLastGlobalSequence )
    {
        this.countForLastGlobalSequence = countForLastGlobalSequence;
    }

    public long getLastDataTime()
    {
        return lastDataTime;
    }

    public void setLastDataTime ( long lastDataTime )
    {
        this.lastDataTime = lastDataTime;
    }

    public long getNextGlobalSequenceUpdateTime()
    {
        return nextGlobalSequenceUpdateTime;
    }

    public void setNextGlobalSequenceUpdateTime ( long nextGlobalSequenceUpdateTime )
    {
        this.nextGlobalSequenceUpdateTime = nextGlobalSequenceUpdateTime;
    }

}
