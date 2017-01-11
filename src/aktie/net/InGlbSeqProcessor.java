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
            Long psq = b.getNumber ( CObj.SEQNUM );
            Long msq = b.getNumber ( CObj.MEMSEQNUM );
            Long ssq = b.getNumber ( CObj.SUBSEQNUM );

            if ( psq != null && msq != null && ssq != null )
            {
                conThread.setLastSeq ( psq, msq, ssq );
            }

            return true;
        }

        return false;
    }

}
