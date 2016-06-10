package aktie.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class DirectoryShare
{

    @Id
    @GeneratedValue
    private long id;
    private String shareName;
    private String communityId;
    private String memberId;

    private String directory;

    private long lastCrawl;
    private long numberFiles;
    private long numberSubFolders;
    private String message;

    private boolean defaultDownload;
    @Column ( columnDefinition = "BOOLEAN(1) NOT NULL default FALSE" )
    private boolean skipSpam;

    @Override
    public int hashCode()
    {
        return ( int ) id;
    }

    @Override
    public boolean equals ( Object o )
    {
        if ( o instanceof DirectoryShare )
        {
            DirectoryShare ds = ( DirectoryShare ) o;

            if ( ds != null )
            {
                return ds.getId() == getId();
            }

        }

        return false;
    }

    public long getId()
    {
        return id;
    }

    public void setId ( long id )
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

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory ( String directory )
    {
        this.directory = directory;
    }

    public long getLastCrawl()
    {
        return lastCrawl;
    }

    public void setLastCrawl ( long lastCrawl )
    {
        this.lastCrawl = lastCrawl;
    }

    public long getNumberFiles()
    {
        return numberFiles;
    }

    public void setNumberFiles ( long numberFiles )
    {
        this.numberFiles = numberFiles;
    }

    public long getNumberSubFolders()
    {
        return numberSubFolders;
    }

    public void setNumberSubFolders ( long numberSubFolders )
    {
        this.numberSubFolders = numberSubFolders;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage ( String message )
    {
        this.message = message;
    }

    public String getShareName()
    {
        return shareName;
    }

    public void setShareName ( String shareName )
    {
        this.shareName = shareName;
    }

    public boolean isDefaultDownload()
    {
        return defaultDownload;
    }

    public void setDefaultDownload ( boolean defaultDownload )
    {
        this.defaultDownload = defaultDownload;
    }

    public boolean isSkipSpam()
    {
        return skipSpam;
    }

    public void setSkipSpam ( boolean skipSpam )
    {
        this.skipSpam = skipSpam;
    }

}
