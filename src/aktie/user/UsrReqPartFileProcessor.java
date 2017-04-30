package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

// HasPart
public class UsrReqPartFileProcessor extends GenericProcessor
{
    public static final long DEFAULT_PRIORITY = 5L;

    private IdentityManager identManager;

    public UsrReqPartFileProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj co )
    {
        String type = co.getType();

        if ( CObj.USR_PARTFILE_UPDATE.equals ( type ) )
        {
            long priority = DEFAULT_PRIORITY;
            Long coPriority = co.getNumber ( CObj.PRIORITY );

            if ( coPriority != null )
            {
                priority = coPriority;
            }

            String communityID = co.getString ( CObj.COMMUNITYID );

            if ( communityID == null )
            {
                identManager.requestAllPartFiles ( ( int ) priority );
            }

            else
            {
                identManager.requestPartFile ( communityID, ( int ) priority );
            }

            return true;
        }

        return false;
    }

}
