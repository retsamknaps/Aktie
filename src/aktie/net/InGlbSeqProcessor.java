package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InGlbSeqProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

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
            Long psq = b.getNumber ( CObj.SEQNUM );
            Long msq = b.getNumber ( CObj.MEMSEQNUM );
            Long ssq = b.getNumber ( CObj.SUBSEQNUM );

            log.info ( "GLB SEQ COMPLETE: ME: " + conThread.getLocalDestination().getIdentity().getId() +
                       " FROM: " + conThread.getEndDestination().getId() + " SEQ: " +
                       psq + " " + msq + " " + ssq );

            if ( psq != null && msq != null && ssq != null )
            {
                conThread.setLastSeq ( psq, msq, ssq );
            }

            return true;
        }

        return false;
    }

}
