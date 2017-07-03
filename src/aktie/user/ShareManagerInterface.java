package aktie.user;

import java.util.List;

import aktie.data.DirectoryShare;

public interface ShareManagerInterface
{

    public void addShare ( String comid, String memid, String name, String dir, boolean def, boolean skipspam );

    public void setShareListener ( ShareListener l );

    public void setEnabled ( boolean enabled );

    public boolean isRunning();

    public boolean isEnabled();

    public DirectoryShare getShare ( String comid, String memid, String name );

    public DirectoryShare getDefaultShare ( String comid, String memid );

    public List<DirectoryShare> listShares ( String comid, String memid );

    public void deleteShare ( String comid, String memid, String name );

    public DirectoryShare getShare ( long id );

}
