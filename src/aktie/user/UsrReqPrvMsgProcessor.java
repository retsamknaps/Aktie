package aktie.user;

import org.hibernate.Session;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.sequences.PrivIdentSequence;
import aktie.sequences.PrivMsgSequence;

public class UsrReqPrvMsgProcessor extends GenericNoContextProcessor
{

    private Index index;
    private HH2Session session;

    public UsrReqPrvMsgProcessor ( HH2Session s, Index ix )
    {
        index = ix;
        session = s;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_PRVMSG_UPDATE.equals ( type ) )
        {
            Session s = null;
            CObjList allident = null;

            try
            {
                s = session.getSession();
                allident = index.getIdentities();

                for ( int c = 0; c < allident.size(); c++ )
                {
                    CObj ident = allident.get ( c );

                    if ( !"true".equals ( ident.getPrivate ( CObj.MINE ) ) )
                    {
                        String i = ident.getId();
                        PrivIdentSequence pseq = new PrivIdentSequence ( session );
                        pseq.request ( s, i, 5, i );
                        PrivMsgSequence mseq = new PrivMsgSequence ( session );
                        mseq.request ( s, i, 5, i );
                    }

                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();

                if ( s != null )
                {
                    try
                    {
                        if ( s.getTransaction().isActive() )
                        {
                            s.getTransaction().rollback();
                        }

                    }

                    catch ( Exception e2 )
                    {
                    }

                }

            }

            finally
            {
                if ( allident != null )
                {
                    try
                    {
                        allident.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

                if ( s != null )
                {
                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

            }

            return true;
        }

        return false;
    }

}
