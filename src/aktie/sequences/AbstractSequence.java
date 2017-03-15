package aktie.sequences;

import org.hibernate.Session;

import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.user.IdentityManager;

public abstract class AbstractSequence<T>
{

    public static int MAXPRIORITY = 20;

    public T Obj;
    private Class<T> Typ;
    private HH2Session session;

    public AbstractSequence ( Class<T> c, HH2Session s )
    {
        Typ = c;
        session = s;
        Obj = null;
    }

    public abstract String getId();
    public abstract void setId ( String id );

    public abstract long getLastNumber();
    public abstract void setLastNumber ( long ln );

    public abstract long getNextClosestNumber();
    public abstract void setNextClosestNumber ( long ln );

    public abstract int getNumClosestNumber();
    public abstract void setNumClosestNumber ( int ln );

    public abstract long getLastUpdate();
    public abstract void setLastUpdate ( long ln );

    public abstract int getStatus();
    public abstract void setStatus ( int ln );

    public abstract int getUpdatePriority();
    public abstract void setUpdatePriority ( int ln );

    public abstract int getUpdateCycle();
    public abstract void setUpdateCycle ( int ln );

    public abstract T createNewObj ( CObj c );
    public abstract T createNewObj ( String... c );

    public HH2Session getSession()
    {
        return session;
    }

    public T getObj()
    {
        return Obj;
    }

    @SuppressWarnings ( "unchecked" )
    public void request ( Session s, String id, int priority, String... c ) throws Exception
    {
        s.getTransaction().begin();
        Obj = ( T )
              s.get ( Typ, id );

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
            if ( getNextClosestNumber() > getLastNumber() &&
                    getNumClosestNumber() > IdentityManager.MAX_LAST_NUMBER )
            {
                setLastNumber ( getNextClosestNumber() );
                setNumClosestNumber ( 0 );
                setUpdateCycle ( 0 );
            }

            if ( getStatus() == CommunityMember.UPDATE &&
                    getUpdatePriority() < MAXPRIORITY )
            {
                setUpdatePriority ( getUpdatePriority() + 1 );
            }

            else
            {
                setUpdatePriority ( priority );
            }

            setStatus ( CommunityMember.UPDATE );
            setUpdateCycle ( getUpdateCycle() + 1 );
            s.merge ( Obj );

        }

        s.getTransaction().commit();
    }

    @SuppressWarnings ( "unchecked" )
    public void updateSequence ( CObj c ) throws Exception
    {
        Long seqnum = c.getNumber ( CObj.SEQNUM );

        if ( seqnum != null )
        {
            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();
                Obj = ( T )
                      s.get ( Typ, getId() );

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
                            //if ( getNextClosestNumber() > seqnum ||
                            //        getNextClosestNumber() <= getLastNumber() )
                            //{
                            //    setNextClosestNumber ( seqnum );
                            //    setUpdateCycle ( 0 );
                            //    setNumClosestNumber ( 1 );
                            //    s.merge ( Obj );
                            //}

                            //
                            //else if ( getNextClosestNumber() == seqnum )
                            //{
                            //    setNumClosestNumber (
                            //        getNumClosestNumber() + 1 );
                            //    s.merge ( Obj );
                            //}

                            //else if ( getUpdateCycle() >= IdentityManager.MAX_UPDATE_CYCLE )
                            //{
                            setUpdateCycle ( 0 );
                            setLastNumber ( seqnum );
                            s.merge ( Obj );
                            //}

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

    public void setObj ( T obj )
    {
        Obj = obj;
    }

}
