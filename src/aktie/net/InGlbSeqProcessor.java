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
                conThread.setLastSeq ( sq );
            }

            return true;
        }

        return false;
    }

}
