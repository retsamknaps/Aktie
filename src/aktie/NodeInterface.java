package aktie;

import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.index.CObjList;
import aktie.index.IndexInterface;
import aktie.net.GetSendData2;
import aktie.net.Net;
import aktie.upgrade.NodeUpgrader;
import aktie.user.RequestFileHandlerInterface;
import aktie.user.ShareManagerInterface;

public interface NodeInterface
{

    public void enqueue ( CObj o );

    public void enqueue ( CObjList o );

    public void priorityEnqueue ( CObj o );

    public void startDestinations ( int delay );

    public IndexInterface getIndex();

    public ShareManagerInterface getShareManager();

    public RequestFileHandlerInterface getFileHandler();

    public void newDeveloperIdentity ( String id );

    public GetSendData2 getConnectionManager();

    public Net getNet();

    public NodeUpgrader getUpgrader();

    public void close();
    
    public DeveloperIdentity getDeveloper(String id);

}
