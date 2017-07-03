package aktie.net;

import java.util.List;
import java.util.Set;

import aktie.data.CObj;
import aktie.data.RequestFile;

public interface GetSendData2
{

    public Object nextNonFile ( String localdest, String remotedest, Set<String> members, Set<String> subs, boolean getNextGlobal );

    public Object nextFile ( String localdest, String remotedest, Set<RequestFile> hasfiles );

    public Set<RequestFile> getHasFileForConnection ( String remotedest, Set<String> subs );

    public long getLastFileUpdate();

    public void closeConnection ( String localdest, String remotedest );

    public void toggleConnectionLogging ( String localdest, String remotedest );

    public List<CObj> getConnectedIdentities();

}
