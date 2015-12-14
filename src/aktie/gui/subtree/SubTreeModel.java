package aktie.gui.subtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;

import aktie.data.CObj;
import aktie.index.Index;

public class SubTreeModel implements ITreeContentProvider
{

    private Map<Long, SubTreeEntity> entities;
    private Map<Long, List<SubTreeEntity>> children;
    private Map<Long, Long> parents;
    private List<SubTreeEntity> sorted;
    private Map<Long, CObj> fullObj;
    private Map<String, SubTreeEntity> idMap;
    private SubTreeEntityDBInterface db;
    private Index index;


    public SubTreeModel ( Index idx, SubTreeEntityDBInterface d )
    {
        db = d;
        index = idx;
        entities = new HashMap<Long, SubTreeEntity>();
        children = new HashMap<Long, List<SubTreeEntity>>();
        sorted = new LinkedList<SubTreeEntity>();
        parents = new HashMap<Long, Long>();
        fullObj = new HashMap<Long, CObj>();
        idMap = new HashMap<String, SubTreeEntity>();
    }

    private synchronized void removeFromChildren ( SubTreeEntity se )
    {
        for ( List<SubTreeEntity> l : children.values() )
        {
            l.remove ( se );
        }

    }

    private synchronized void addSubTreeElement ( SubTreeEntity se )
    {
        entities.put ( se.getId(), se );
        removeFromChildren ( se );
        List<SubTreeEntity> cl = children.get ( se.getParent() );

        if ( cl == null )
        {
            cl = new LinkedList<SubTreeEntity>();
            children.put ( se.getParent(), cl );
        }

        cl.add ( se );
        Collections.sort ( cl );
        parents.put ( se.getId(), se.getParent() );

        if ( !sorted.contains ( se ) )
        {
            sorted.add ( se );
        }

        Collections.sort ( sorted );
        idMap.put ( se.getRefId(), se );
    }

    public synchronized void init()
    {
        List<SubTreeEntity> l = db.getEntities();

        for ( SubTreeEntity e : l )
        {
            addSubTreeElement ( e );
        }

    }

    private synchronized void saveEntities()
    {
        db.saveAll ( sorted );
    }

    private void displayAll()
    {
        System.out.println ( "=================================================" );

        for ( SubTreeEntity e : sorted )
        {
            System.out.println ( "id: " + e.getId() + " prt: " + e.getParent() + " txt: " + e.getText() + " sorder: " + e.getSortOrder() );
        }

        System.out.println ( "=================================================" );
    }

    public synchronized CObj getCObj ( long id )
    {
        return fullObj.get ( id );
    }

    public synchronized CObj getCObj ( String id )
    {
        SubTreeEntity e = idMap.get ( id );

        if ( e != null )
        {
            return getCObj ( e.getId() );
        }

        return null;
    }

    public synchronized void update ( CObj c )
    {
        System.out.println ( "UPDATE!!!!!!!! " );

        if ( CObj.IDENTITY.equals ( c.getType() ) )
        {
            //If we don't have an entity for it, create it.
            SubTreeEntity se = idMap.get ( c.getId() );

            if ( se == null )
            {
                se = new SubTreeEntity();
                se.setIdentity ( c.getId() );
                se.setRefId ( c.getId() );
                se.setType ( SubTreeEntity.IDENTITY_TYPE );
                se.setText ( c.getDisplayName() );
            }

            //update the on/off state of the entity
            Long on = c.getPrivateNumber ( CObj.PRV_DEST_OPEN );

            if ( on == null || on == 1L )
            {
                se.setConnected ( true );
            }

            else
            {
                se.setConnected ( false );
            }

            db.saveEntity ( se );
            fullObj.put ( se.getId(), c );
            addSubTreeElement ( se );
        }

        if ( CObj.SUBSCRIPTION.equals ( c.getType() ) )
        {
            String cid = c.getString ( CObj.CREATOR );
            String comid = c.getString ( CObj.COMMUNITYID );
            String mcid = cid + comid;

            if ( "true".equals ( c.getString ( CObj.SUBSCRIBED ) ) )
            {
                CObj com = index.getCommunity ( comid );

                if ( com != null )
                {
                    SubTreeEntity se = idMap.get ( mcid );
                    SubTreeEntity prt = idMap.get ( cid );

                    if ( se == null && prt != null )
                    {
                        se = new SubTreeEntity();
                        se.setIdentity ( cid );
                        se.setRefId ( mcid );
                        se.setText ( com.getPrivateDisplayName() );

                        if ( CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) )
                        {
                            se.setType ( SubTreeEntity.PRVCOMMUNITY_TYPE );
                        }

                        else
                        {
                            se.setType ( SubTreeEntity.PUBCOMMUNITY_TYPE );
                        }

                        System.out.println ( "ident: " + cid + " has id: " + prt.getId() + " obj: " + prt + " txt: " + prt.getText() );
                        se.setParent ( prt.getId() );
                        db.saveEntity ( se );
                        fullObj.put ( se.getId(), com );
                        addSubTreeElement ( se );
                    }

                }

            }

            else
            {
                //Remove the subscription
                SubTreeEntity se = idMap.remove ( mcid );

                if ( se != null )
                {
                    removeFromChildren ( se );
                    sorted.remove ( se );
                    parents.remove ( se.getId() );
                    entities.remove ( se.getId() );
                    db.deleteElement ( se );
                }

            }

        }

        displayAll();
    }

    private synchronized void putToLocation ( int idx, SubTreeEntity ent )
    {
        long orderval = 0;

        if ( idx > 0 )
        {
            SubTreeEntity s = sorted.get ( idx - 1 );
            orderval = s.getSortOrder() + 1;
        }

        ent.setSortOrder ( orderval );
        sorted.add ( idx, ent );
        orderval++;
        idx++;

        if ( idx < sorted.size() )
        {
            ListIterator<SubTreeEntity> i = sorted.listIterator ( idx );

            while ( i.hasNext() )
            {
                SubTreeEntity st = i.next();
                st.setSortOrder ( orderval );
                orderval++;
            }

        }

        updateParentChild ( ent );
    }

    private synchronized void updateParentChild ( SubTreeEntity se )
    {
        parents.put ( se.getId(), se.getParent() );
        List<SubTreeEntity> cl = children.get ( se.getParent() );

        if ( cl == null )
        {
            cl = new LinkedList<SubTreeEntity>();
            children.put ( se.getParent(), cl );
        }

        cl.add ( se );
        Collections.sort ( cl );
    }

    public synchronized void dropped ( Object dstr, Object target, int dir )
    {
        if ( dstr != null && dstr instanceof String &&
                target != null && target instanceof SubTreeEntity )
        {
            int tolocation = 0;
            SubTreeEntity dropent = entities.get ( Long.valueOf ( ( String ) dstr ) );
            SubTreeEntity tarent = ( SubTreeEntity ) target;
            //Remove from children list
            List<SubTreeEntity> cl = children.get ( dropent.getParent() );

            if ( cl != null )
            {
                cl.remove ( dropent );
            }

            parents.remove ( dropent.getId() );
            sorted.remove ( dropent );

            if ( dropent.getIdentity().equals ( tarent.getIdentity() ) )
            {
                if ( dir == ViewerDropAdapter.LOCATION_ON ||
                        dir == ViewerDropAdapter.LOCATION_AFTER )
                {
                    if ( tarent.getType() == SubTreeEntity.FOLDER_TYPE ||
                            tarent.getType() == SubTreeEntity.IDENTITY_TYPE )
                    {
                        dropent.setParent ( tarent.getId() );
                    }

                    else
                    {
                        dropent.setParent ( tarent.getParent() );
                    }

                    tolocation = sorted.indexOf ( tarent );
                    tolocation += 1;
                }

                else
                {
                    dropent.setParent ( tarent.getParent() );
                    tolocation = sorted.indexOf ( tarent );
                }

            }

            else
            {
                if ( dir == ViewerDropAdapter.LOCATION_ON ||
                        dir == ViewerDropAdapter.LOCATION_AFTER )
                {
                    tolocation = sorted.indexOf ( tarent );
                    tolocation += 1;
                }

                else
                {
                    tolocation = sorted.indexOf ( tarent );
                }

            }

            putToLocation ( tolocation, dropent );
            displayAll();
        }

        saveEntities();
    }

    @Override
    public void dispose()
    {
    }


    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override

    public synchronized Object[] getChildren ( Object o )
    {
        if ( o != null && o instanceof SubTreeEntity )
        {
            SubTreeEntity s = ( SubTreeEntity ) o;
            List<SubTreeEntity> cd = children.get ( s.getId() );

            if ( cd != null )
            {
                Object r[] = cd.toArray();
                System.out.println ( "Children of: " + s.getText() + " size: " + r.length );
                return r;
            }

        }

        return new Object[] {};

    }


    @Override

    public synchronized Object[] getElements ( Object o )
    {
        System.out.println ( "!!!!!!!!!!!! getElements() " + o );
        List<SubTreeEntity> rl = new ArrayList<SubTreeEntity>();
        Iterator<SubTreeEntity> i = sorted.iterator();

        while ( i.hasNext() )
        {
            SubTreeEntity s = i.next();

            if ( s.getType() == SubTreeEntity.IDENTITY_TYPE )
            {
                rl.add ( s );
            }

        }

        return rl.toArray();
    }


    @Override

    public synchronized Object getParent ( Object o )
    {
        if ( o != null && o instanceof SubTreeEntity )
        {
            SubTreeEntity s = ( SubTreeEntity ) o;
            Long pid = parents.get ( s.getId() );

            if ( pid != null )
            {
                return entities.get ( pid );
            }

        }

        return null;
    }


    @Override

    public synchronized boolean hasChildren ( Object o )
    {
        if ( o != null && o instanceof SubTreeEntity )
        {
            SubTreeEntity s = ( SubTreeEntity ) o;
            List<SubTreeEntity> cd = children.get ( s.getId() );
            boolean haschil = cd != null && !cd.isEmpty();
            System.out.println ( "Has children: " + s.getText() + " ? " + haschil );
            return haschil;
        }

        return false;
    }


}
