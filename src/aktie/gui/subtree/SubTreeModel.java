package aktie.gui.subtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;

import aktie.data.CObj;
import aktie.index.Index;

public class SubTreeModel implements ITreeContentProvider
{

    public static int POST_TREE = 0;
    public static int MESSAGE_TREE = 1;

    private Map<Long, SubTreeEntity> entities;
    private Map<Long, List<SubTreeEntity>> children;
    private Map<Long, Long> parents;
    private List<SubTreeEntity> sorted;
    private Map<Long, CObj> fullObj;
    private Map<String, SubTreeEntity> idMap;
    private SubTreeEntityDBInterface db;
    private Index index;
    private int type;
    private int treeId;


    public SubTreeModel ( Index idx, SubTreeEntityDBInterface d, int t, int tid )
    {
        db = d;
        index = idx;
        type = t;
        treeId = tid;
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
        if ( se.getType() == SubTreeEntity.IDENTITY_TYPE )
        {
            se.setParent ( 0 );
        }

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
        List<SubTreeEntity> l = db.getEntities ( treeId );

        for ( SubTreeEntity e : l )
        {
            addSubTreeElement ( e );
        }

    }

    private synchronized void saveEntities()
    {
        db.saveAll ( sorted );
    }

    /*
        private void displayAll()
        {
        System.out.println ( "=================================================" );

        for ( SubTreeEntity e : sorted )
        {
            System.out.println ( "id: " + e.getId() + " prt: " + e.getParent() + " txt: " + e.getText() + " sorder: " + e.getSortOrder() );
        }

        System.out.println ( "=================================================" );
        }*/

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

    public synchronized void removeFolder ( SubTreeEntity e )
    {
        if ( e != null && e.getType() == SubTreeEntity.FOLDER_TYPE )
        {
            removeFromChildren ( e );
            entities.remove ( e.getId() );
            parents.remove ( e.getId() );
            fullObj.remove ( e.getId() );
            idMap.remove ( e.getRefId() );
            sorted.remove ( e );

            List<SubTreeEntity> chlst = children.get ( e.getParent() );

            if ( chlst != null )
            {
                for ( SubTreeEntity s : sorted )
                {
                    if ( s.getParent() == e.getId() )
                    {
                        s.setParent ( e.getParent() );
                        chlst.add ( s );
                    }

                }

                Collections.sort ( chlst );
            }

            for ( Entry<Long, Long> t : parents.entrySet() )
            {
                if ( t.getValue() == e.getId() )
                {
                    t.setValue ( e.getParent() );
                }

            }

            saveEntities();
            db.deleteElement ( e );
        }

    }

    public synchronized void addFolder ( SubTreeEntity parent, String name )
    {
        SubTreeEntity ne = new SubTreeEntity();
        ne.setTreeId ( treeId );
        ne.setType ( SubTreeEntity.FOLDER_TYPE );
        ne.setText ( name );

        if ( SubTreeEntity.FOLDER_TYPE == parent.getType() ||
                SubTreeEntity.IDENTITY_TYPE == parent.getType() )
        {
            ne.setParent ( parent.getId() );
        }

        else
        {
            ne.setParent ( parent.getParent() );
        }

        ne.setIdentity ( parent.getIdentity() );
        db.saveEntity ( ne );
        addSubTreeElement ( ne );
    }

    private synchronized void setBlue ( SubTreeEntity e, boolean t )
    {
        if ( e != null )
        {
            e.setBlue ( t );
            db.saveEntity ( e );

            if ( e.getId() != e.getParent() )
            {
                SubTreeEntity p = entities.get ( e.getParent() );
                setBlue ( p, t );
            }

        }

    }

    public synchronized void clearBlue ( CObj com )
    {
        if ( com != null )
        {
            for ( Entry<Long, CObj> et : fullObj.entrySet() )
            {
                CObj ob = et.getValue();

                if ( ob.getDig().equals ( com.getDig() ) )
                {
                    SubTreeEntity ste = entities.get ( et.getKey() );
                    setBlue ( ste, false );
                }

            }

        }

    }

    public synchronized void clearBlueMessages ( CObj msg )
    {
        if ( msg != null )
        {
            String mid = msg.getPrivate ( CObj.PRV_MSG_ID );

            if ( mid != null )
            {
                SubTreeEntity ste = idMap.get ( mid );
                setBlue ( ste, false );
            }

        }

    }

    public synchronized void update ( CObj c )
    {
        if ( type == POST_TREE )
        {
            if ( CObj.POST.equals ( c.getType() ) )
            {
                String comid = c.getString ( CObj.COMMUNITYID );

                for ( Entry<Long, CObj> et : fullObj.entrySet() )
                {
                    CObj ob = et.getValue();

                    if ( ob.getDig().equals ( comid ) )
                    {
                        SubTreeEntity ste = entities.get ( et.getKey() );
                        setBlue ( ste, true );
                    }

                }

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
                            se.setTreeId ( treeId );
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

                            se.setParent ( prt.getId() );
                            db.saveEntity ( se );
                        }

                        fullObj.put ( se.getId(), com );
                        addSubTreeElement ( se );

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

        }

        if ( type == MESSAGE_TREE )
        {
            if ( CObj.PRIVIDENTIFIER.equals ( c.getType() ) )
            {
                String creator = c.getString ( CObj.CREATOR );
                String recip = c.getPrivate ( CObj.PRV_RECIPIENT );
                String msgid = c.getPrivate ( CObj.PRV_MSG_ID );
                boolean mine = ( "true".equals ( c.getPrivate ( CObj.MINE ) ) );

                if ( creator != null && recip != null && msgid != null )
                {
                    String lid = recip;

                    if ( mine )
                    {
                        lid = creator;
                    }

                    SubTreeEntity le = idMap.get ( lid );
                    SubTreeEntity re = idMap.get ( msgid );

                    if ( le != null && re == null )
                    {
                        re = new SubTreeEntity();
                        re.setTreeId ( treeId );
                        re.setIdentity ( lid );
                        re.setRefId ( msgid );
                        re.setText ( c.getPrivateDisplayName() );
                        re.setParent ( le.getId() );
                        db.saveEntity ( re );
                        fullObj.put ( re.getId(), c );
                        addSubTreeElement ( re );
                    }

                }

            }

            if ( CObj.PRIVMESSAGE.equals ( c.getType() ) )
            {
                String msgid = c.getPrivate ( CObj.PRV_MSG_ID );

                if ( msgid != null )
                {
                    SubTreeEntity re = idMap.get ( msgid );

                    if ( re != null )
                    {
                        setBlue ( re, true );
                    }

                }

            }

        }

        if ( CObj.IDENTITY.equals ( c.getType() ) )
        {
            //If we don't have an entity for it, create it.
            SubTreeEntity se = idMap.get ( c.getId() );

            if ( se == null )
            {
                se = new SubTreeEntity();
                se.setTreeId ( treeId );
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


        //displayAll();
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
            SubTreeEntity tarent = ( SubTreeEntity ) target;
            long did = Long.valueOf ( ( String ) dstr );

            //Don't drop to one's self
            if ( tarent.getId() == did )
            {
                return;
            }

            SubTreeEntity dropent = entities.get ( did );
            //Remove from children list
            List<SubTreeEntity> cl = children.get ( dropent.getParent() );

            if ( cl != null )
            {
                cl.remove ( dropent );
            }

            parents.remove ( dropent.getId() );
            sorted.remove ( dropent );

            if ( dropent.getIdentity().equals ( tarent.getIdentity() ) &&
                    dropent.getType() != SubTreeEntity.IDENTITY_TYPE )
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
            //displayAll();
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
                return r;
            }

        }

        return new Object[] {};

    }


    @Override

    public synchronized Object[] getElements ( Object o )
    {
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

    public synchronized void setCollaspseState ( TreeViewer v )
    {
        for ( SubTreeEntity e : sorted )
        {
            v.setExpandedState ( e, !e.isCollapsed() );
        }

    }

    public synchronized void setCollapsed ( SubTreeEntity e, boolean collapse )
    {
        e.setCollapsed ( collapse );
        addSubTreeElement ( e );
        db.saveEntity ( e );
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
            return haschil;
        }

        return false;
    }


}
