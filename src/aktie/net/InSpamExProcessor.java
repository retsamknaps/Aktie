package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.sequences.SpamSequence;
import aktie.spam.SpamTool;
import aktie.utils.DigestValidator;

public class InSpamExProcessor extends GenericProcessor
{

    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private ConnectionThread connection;

    public InSpamExProcessor ( HH2Session s, Index i, SpamTool st, ConnectionThread ct )
    {
        index = i;
        session = s;
        connection = ct;
        validator = new DigestValidator ( index, st );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.SPAMEXCEPTION.equals ( type ) || CObj.USR_SPAMEX.equals ( type ) )
        {
            b.setType ( CObj.SPAMEXCEPTION );

            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );

                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );

                if ( creator != null && seqnum != null )
                {

                    try
                    {
                        SpamSequence sseq = new SpamSequence ( session );
                        sseq.setId ( creator );
                        sseq.updateSequence ( b );

                        //sseq.getObj() is only set if a prior developer identity was added
                        if ( sseq.getObj() != null && isnew )
                        {
                            index.index ( b );
                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();

                    }

                }

            }

            return true;
        }

        return false;
    }


}
