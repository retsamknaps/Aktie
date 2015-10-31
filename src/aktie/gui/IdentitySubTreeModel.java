package aktie.gui;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import aktie.data.CObj;
import aktie.index.Index;

public class IdentitySubTreeModel
{

    private SortedMap<String, CObj> identities;
    private SortedMap<String, SortedMap<String, CObj>> subCommunities;
    private Index index;
    private SWTApp app;


    public IdentitySubTreeModel ( SWTApp ap )
    {
        app = ap;
        index = ap.getNode().getIndex();
        identities = new TreeMap<String, CObj>();
        subCommunities = new TreeMap<String, SortedMap<String, CObj>> ( new Comparator<String>()
        {
            @Override
            public int compare ( String o1, String o2 )
            {
                CObj co1 = identities.get ( o1 );
                CObj co2 = identities.get ( o2 );

                if ( co1 != null && co2 != null )
                {
                    String n1 = co1.getDisplayName();
                    String n2 = co2.getDisplayName();

                    if ( n1 != null && n2 != null )
                    {
                        return n1.compareTo ( n2 );
                    }

                }

                return 0;
            }

        } );

    }

    public void clearNew ( CObj c )
    {
        String comid = c.getDig();

        if ( CObj.COMMUNITY.equals ( c.getType() ) && comid != null )
        {
            //loop through subCommunities to look for community
            Iterator<Entry<String, SortedMap<String, CObj>>> it = subCommunities.entrySet().iterator();

            while ( it.hasNext() )
            {
                Entry<String, SortedMap<String, CObj>> mp = it.next();
                Iterator<CObj> ci = mp.getValue().values().iterator();
                boolean newstuff = false;

                while ( ci.hasNext() )
                {
                    CObj co = ci.next();

                    if ( comid.equals ( co.getDig() ) )
                    {
                        co.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 0L );
                    }

                    else
                    {
                        Long tn = co.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

                        if ( tn != null && tn == 1L )
                        {
                            newstuff = true;
                        }

                    }

                }

                if ( !newstuff )
                {
                    identities.get ( mp.getKey() ).pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 0L );
                }

            }

        }

    }

    public void update ( CObj c )
    {

        if ( CObj.POST.equals ( c.getType() ) )
        {
            //loop through subCommunities to look for community
            String comid = c.getString ( CObj.COMMUNITYID );

            if ( comid != null )
            {
                if ( app.getSelectedCommunity() == null ||
                        ( !comid.equals ( app.getSelectedCommunity().getDig() ) ) )
                {
                    Iterator<Entry<String, SortedMap<String, CObj>>> it = subCommunities.entrySet().iterator();

                    while ( it.hasNext() )
                    {
                        Entry<String, SortedMap<String, CObj>> mp = it.next();
                        Iterator<CObj> ci = mp.getValue().values().iterator();
                        boolean newstuff = false;

                        while ( ci.hasNext() )
                        {
                            CObj co = ci.next();

                            if ( comid.equals ( co.getDig() ) )
                            {
                                co.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 1L );
                                newstuff = true;
                            }

                        }

                        if ( newstuff )
                        {
                            identities.get ( mp.getKey() ).pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 1L );
                        }

                    }

                }

            }

        }

        if ( CObj.IDENTITY.equals ( c.getType() ) )
        {
            identities.put ( c.getId(), c );
            SortedMap<String, CObj> sm = subCommunities.get ( c.getId() );

            if ( sm == null )
            {
                sm = new TreeMap<String, CObj> ( new Comparator<String>()
                {

                    @Override
                    public int compare ( String o1, String o2 )
                    {
                        CObj co1 = index.getByDig ( o1 );
                        CObj co2 = index.getByDig ( o2 );

                        if ( co1 != null && co2 != null )
                        {
                            String n1 = co1.getPrivateDisplayName();
                            String n2 = co2.getPrivateDisplayName();

                            if ( n1 != null && n2 != null )
                            {
                                return n1.compareTo ( n2 );
                            }

                        }

                        return 0;
                    }

                } );

                subCommunities.put ( c.getId(), sm );
            }

        }

        if ( CObj.SUBSCRIPTION.equals ( c.getType() ) )
        {
            String cid = c.getString ( CObj.CREATOR );
            SortedMap<String, CObj> sm = subCommunities.get ( cid );

            if ( sm == null )
            {
                sm = new TreeMap<String, CObj> ( new Comparator<String>()
                {

                    @Override
                    public int compare ( String o1, String o2 )
                    {
                        CObj co1 = index.getByDig ( o1 );
                        CObj co2 = index.getByDig ( o2 );

                        if ( co1 != null && co2 != null )
                        {
                            String n1 = co1.getPrivateDisplayName();
                            String n2 = co2.getPrivateDisplayName();

                            if ( n1 != null && n2 != null )
                            {
                                return n1.compareTo ( n2 );
                            }

                        }

                        return 0;
                    }

                } );

                subCommunities.put ( cid, sm );
            }

            if ( "true".equals ( c.getString ( CObj.SUBSCRIBED ) ) )
            {
                CObj com = index.getCommunity ( c.getString ( CObj.COMMUNITYID ) );

                if ( com != null )
                {
                    sm.put ( com.getDig(), com );
                }

            }

            else
            {
                sm.remove ( c.getString ( CObj.COMMUNITYID ) );
            }

        }

    }

    public Map<String, CObj> getIdentities()
    {
        return identities;
    }

    public Map<String, SortedMap<String, CObj>> getSubCommunities()
    {
        return subCommunities;
    }

    public Index getIndex()
    {
        return index;
    }

}
