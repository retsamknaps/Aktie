package aktie.sequences;

import org.hibernate.Session;

import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.user.IdentityManager;

public class SpamSequence extends AbstractSequence<DeveloperIdentity>
{

    public SpamSequence ( HH2Session s )
    {
        super ( DeveloperIdentity.class, s );
    }

    @Override
    public String getId()
    {
        if ( getObj() != null )
        {
            return getObj().getId();
        }

        return staticid;
    }

    private String staticid;
    @Override
    public void setId ( String id )
    {
        if ( getObj() != null )
        {
            getObj().setId ( id );
        }

        staticid = id;
    }

    @Override
    public long getLastNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getLastSpamExNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastSpamExNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestSpamExNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestSpamExNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestSpamExNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestSpamExNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastSpamExUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastSpamExUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getSpamExStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSpamExStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            return getObj().getSpamExUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSpamExUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            return getObj().getSpamExUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSpamExUpdateCycle ( ln );
        }

    }

    @Override
    public DeveloperIdentity createNewObj ( CObj c )
    {
        return null;
    }



    @Override
    public DeveloperIdentity createNewObj ( String... c )
    {
        return null;
    }

    public void updateSequence ( CObj c ) throws Exception
    {
        Long seqnum = c.getNumber ( CObj.SEQNUM );

        if ( seqnum != null )
        {
            Session s = null;

            try
            {
                s = getSession().getSession();
                s.getTransaction().begin();
                Obj = ( DeveloperIdentity )
                      s.get ( DeveloperIdentity.class, getId() );

                if ( Obj == null )
                {
                    Obj = createNewObj ( c );

                    if ( Obj != null )
                    {
                        s.persist ( Obj );
                    }

                }

                if ( Obj != null )
                {
                    if ( getLastNumber() + 1 == ( long ) seqnum )
                    {
                        setLastNumber ( seqnum );
                        setNextClosestNumber ( seqnum );
                        setUpdateCycle ( 0 );
                        setNumClosestNumber ( 1 );
                        s.merge ( Obj );
                    }

                    else
                    {
                        /*
                                if there is a permanent gap in a sequence number
                                count how many times we see the next number, so
                                if we see it too many times we just use it for last
                                number instead
                        */
                        if ( seqnum > getLastNumber() )
                        {
                            if ( getNextClosestNumber() > seqnum ||
                                    getNextClosestNumber() <= getLastNumber() )
                            {
                                setNextClosestNumber ( seqnum );
                                setUpdateCycle ( 0 );
                                setNumClosestNumber ( 1 );
                                s.merge ( Obj );
                            }


                            else if ( getNextClosestNumber() == seqnum )
                            {
                                setNumClosestNumber (
                                    getNumClosestNumber() + 1 );
                                s.merge ( Obj );
                            }

                            else if ( getUpdateCycle() >= IdentityManager.MAX_UPDATE_CYCLE )
                            {
                                setUpdateCycle ( 0 );
                                setLastNumber ( seqnum );
                                s.merge ( Obj );
                            }

                        }

                    }

                }

                s.getTransaction().commit();

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

                throw new Exception ( "Transaction failure.", e );

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

}
