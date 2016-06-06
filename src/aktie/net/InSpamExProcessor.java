package aktie.net;

import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.DigestValidator;

public class InSpamExProcessor extends GenericProcessor
{

    private Index index;
    private HH2Session session;
    private DigestValidator validator;

    public InSpamExProcessor ( HH2Session s, Index i, SpamTool st )
    {
        index = i;
        session = s;
        validator = new DigestValidator ( index, st );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.SPAMEXCEPTION.equals ( type ) )
        {
            if ( validator.newAndValid ( b ) )
            {
                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );

                if ( creator != null && seqnum != null )
                {
                    DeveloperIdentity m = null;

                    Session s = null;

                    try
                    {
                        s = session.getSession();
                        s.getTransaction().begin();
                        m = ( DeveloperIdentity )
                            s.get ( DeveloperIdentity.class, creator );

                        if ( m != null )
                        {
                            if ( m.getLastSpamExNumber() + 1 == ( long ) seqnum )
                            {
                                m.setLastSpamExNumber ( seqnum );
                                m.setNextClosestSpamExNumber ( seqnum );
                                m.setNumClosestSpamExNumber ( 1 );
                                s.merge ( m );
                            }

                            else
                            {
                                /*
                                        if there is a permanent gap in a sequence number
                                        count how many times we see the next number, so
                                        if we see it too many times we just use it for last
                                        number instead
                                */
                                if ( seqnum > m.getLastSpamExNumber() )
                                {
                                    if ( m.getNextClosestSpamExNumber() > seqnum ||
                                            m.getNextClosestSpamExNumber() <= m.getLastSpamExNumber() )
                                    {
                                        m.setNextClosestSpamExNumber ( seqnum );
                                        m.setNumClosestSpamExNumber ( 1 );
                                        s.merge ( m );
                                    }

                                    else if ( m.getNextClosestSpamExNumber() == seqnum )
                                    {
                                        m.setNumClosestSpamExNumber (
                                            m.getNumClosestSpamExNumber() + 1 );
                                        s.merge ( m );
                                    }

                                }

                            }

                        }

                        s.getTransaction().commit();

                        if ( m != null )
                        {
                            index.index ( b );
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
                        if ( s != null )
                        {
                            try
                            {
                                s.close();
                            }

                            catch ( Exception e )
                            {
                                e.printStackTrace();
                            }

                        }

                    }

                }

            }

            return true;
        }

        return false;
    }


}
