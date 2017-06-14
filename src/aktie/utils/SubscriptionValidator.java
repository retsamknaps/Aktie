package aktie.utils;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class SubscriptionValidator
{

    private Index index;

    public SubscriptionValidator ( Index idx )
    {
        index = idx;
    }

    public CObj isMyUserSubscribed ( String comid, String creator )
    {
        //Get the creator and make sure it is this user
        if ( creator == null )
        {
            return null;
        }

        CObj myid = index.getMyIdentity ( creator );

        if ( myid == null )
        {
            return null;
        }

        //Make sure subscribed
        if ( comid == null )
        {
            return null;
        }

        CObj subscription = index.getSubscription ( comid, creator );

        if ( subscription == null )
        {
            return null;
        }

        if ( !"true".equals ( subscription.getString ( CObj.SUBSCRIBED ) ) )
        {
            return null;
        }

        return myid;
    }

    public CObj isUserSubscribed ( String comid, String creator )
    {
        //Get the creator and make sure it is this user
        if ( creator == null )
        {
            return null;
        }

        CObj myid = index.getIdentity ( creator );

        if ( myid == null )
        {
            return null;
        }

        //Make sure subscribed
        if ( comid == null )
        {
            return null;
        }

        CObj subscription = index.getSubscription ( comid, creator );

        if ( subscription == null )
        {
            return null;
        }

        if ( !"true".equals ( subscription.getString ( CObj.SUBSCRIBED ) ) )
        {
            return null;
        }

        return myid;
    }

    public boolean canSubscribe ( String comid, String creator )
    {
        CObj community = index.getCommunity ( comid );

        if ( community == null ) { return false; }

        if ( CObj.SCOPE_PUBLIC.equals ( community.getString ( CObj.SCOPE ) ) ) { return true; }

        //If we're the creator return true
        if ( creator.equals ( community.getString ( CObj.CREATOR ) ) ) { return true; }

        //Ok, not public, see if membership
        //Note, we keep all membership objects, but getMembership will only
        //return memberships that we have been able to decode because it searches
        //the private memberid and communityid fields.
        CObjList ml = index.getMembership ( comid, creator );
        boolean ismem = ( ml.size() > 0 );
        ml.close();
        return ismem;
    }

    public boolean canHasFile ( String comid, String creator, String wdig, String pdig )
    {
        boolean canhas = true;
        CObj community = index.getCommunity ( comid );

        if ( community != null && creator != null )
        {
            if ( "true".equals ( community.getString ( CObj.BLOGMODE ) ) )
            {
                String comcreator = community.getString ( CObj.CREATOR );
                canhas = creator.equals ( comcreator );

                if ( !canhas )
                {
                    //We can have the file is the community creator does
                    CObj o = index.getIdentHasFile ( comid, comcreator, wdig, pdig );

                    if ( o != null && "true".equals ( o.getString ( CObj.STILLHASFILE ) ) )
                    {
                        canhas = true;
                    }

                }

            }

        }

        return canhas;

    }

    public boolean canPost ( String comid, String creator )
    {
        boolean canpst = true;
        CObj community = index.getCommunity ( comid );

        if ( community != null && creator != null )
        {
            if ( "true".equals ( community.getString ( CObj.BLOGMODE ) ) )
            {
                canpst = creator.equals ( community.getString ( CObj.CREATOR ) );
            }

        }

        return canpst;
    }

}
