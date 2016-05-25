package aktie.net;

import aktie.data.CObj;

public class ConnectionElement
{

    public String localId = "";
    public String remoteDest = "";
    public String lastSent = "";
    public String lastRead = "";
    public String mode = "";
    public long pending = 0L;
    public long download = 0L;
    public long upload = 0L;
    public long time = 0L;
    public String upFile = "";
    public String downFile = "";
    public String fulllocalid = "";
    public String fullremoteid = "";

    public ConnectionElement ( ConnectionThread ct )
    {
        if ( ct != null )
        {
            remoteDest = "Connecting/Authenticating";
            CObj id = ct.getEndDestination();

            if ( id != null )
            {
                remoteDest = id.getDisplayName();
                fullremoteid = id.getId();
            }

            localId = "?";

            if ( ct.getLocalDestination() != null
                    && ct.getLocalDestination().getIdentity() != null )
            {
                fullremoteid = ct.getLocalDestination().getIdentity().getId();
                localId = ct.getLocalDestination().getIdentity()
                          .getDisplayName();
            }

            lastSent = ct.getLastSent();

            if ( lastSent == null )
            {
                lastSent = "";
            }

            StringBuilder sb = new StringBuilder();
            lastRead = ct.getLastRead();

            if ( lastRead != null )
            {
                sb.append ( lastRead );
            }

            sb.append ( " " ).append ( ct.getListCount() );
            lastRead = sb.toString();
            mode = ct.isFileMode() ? "FILE" : "norm";
            pending = ct.getPendingFileRequests();
            download = ct.getInBytes();
            upload = ct.getOutBytes();
            long curtime = System.currentTimeMillis();
            time = curtime - ct.getStartTime();
            time = time / ( 1000L );
            upFile = ct.getFileUp() != null ? ct.getFileUp() : "";
            downFile = ct.getFileDown() != null ? ct.getFileDown() : "";
        }

    }

}
