package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InGlbSeqProcessor extends GenericProcessor
{

    private ConnectionThread conThread;

    public InGlbSeqProcessor ( ConnectionThread ct )
    {
        conThread = ct;
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.SEQCOMP.equals ( b.getType() ) )
        {
            Long sq = b.getNumber ( CObj.SEQNUM );

            if ( sq != null )
            {
                System.out.println ( "DONE GETTING GL SEQ: " + conThread.getLocalDestination().getIdentity().getId() + " from: " +
                                     conThread.getEndDestination().getId() + " NUM: " + sq );
                conThread.setLastSeq ( sq );
            }

            return true;
        }

        return false;
    }

}
