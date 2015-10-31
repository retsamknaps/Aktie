package aktie.utils;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class MembershipValidator
{

    private Index index;

    public MembershipValidator ( Index idx )
    {
        index = idx;
    }

    public CObj canGrantMemebership ( String comid, String creator, long ath )
    {
        CObj com = index.getCommunity ( comid );

        if ( com == null ) { return null; }

        if ( !CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) ) { return null; }

        String comc = com.getString ( CObj.CREATOR );

        if ( comc == null ) { return null; }

        if ( comc.equals ( creator ) ) { return com; }

        boolean ok = false;
        CObjList ml = index.getMembership ( comid, creator );

        for ( int c = 0; c < ml.size(); c++ )
        {
            try
            {
                CObj m = ml.get ( c );
                Long as = m.getPrivateNumber ( CObj.AUTHORITY );

                if ( as != null )
                {
                    if ( as == CObj.MEMBER_SUPER || as > ath )
                    {
                        ok = true;
                    }

                }

            }

            catch ( Exception e )
            {
            }

        }

        ml.close();

        if ( ok ) { return com; }

        return null;
    }

}
