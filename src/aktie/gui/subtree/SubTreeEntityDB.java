package aktie.gui.subtree;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.data.HH2Session;

public class SubTreeEntityDB implements SubTreeEntityDBInterface
{

    private HH2Session session;

    public SubTreeEntityDB ( HH2Session s )
    {
        session = s;
    }

    @Override
    public void saveEntity ( SubTreeEntity e )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            SubTreeEntity ne = ( SubTreeEntity ) s.merge ( e );
            e.setId ( ne.getId() );
            s.getTransaction().commit();
        }

        catch ( Exception ex )
        {
            ex.printStackTrace();

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

                catch ( Exception e2 )
                {
                }

            }

        }

    }


    @Override
    public void saveAll ( List<SubTreeEntity> lst )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();

            for ( SubTreeEntity et : lst )
            {
                s.merge ( et );
            }

            s.getTransaction().commit();
        }

        catch ( Exception ex )
        {
            ex.printStackTrace();

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

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public List<SubTreeEntity> getEntities ( int id )
    {
        List<SubTreeEntity> r = new LinkedList<SubTreeEntity>();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM SubTreeEntity x WHERE x.treeId = :tid" );
            q.setParameter ( "tid", id );
            r = q.list();
        }

        catch ( Exception ex )
        {
            ex.printStackTrace();

        }

        finally
        {
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

        return r;
    }

    @Override
    public void deleteElement ( SubTreeEntity e )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            e = ( SubTreeEntity ) s.get ( SubTreeEntity.class, e.getId() );
            s.delete ( e );
            s.getTransaction().commit();
        }

        catch ( Exception ex )
        {
            ex.printStackTrace();

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

                catch ( Exception e2 )
                {
                }

            }

        }

    }

}
