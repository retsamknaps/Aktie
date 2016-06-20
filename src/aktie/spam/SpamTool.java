package aktie.spam;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import aktie.data.CObj;
import aktie.gui.Wrapper;
import aktie.index.Index;
import aktie.utils.HasFileCreator;

public class SpamTool
{

    public static String EXSPAMPREFIX = "EXSPAM";

    private Index index;

    public SpamTool ( Index i )
    {
        index = i;
    }

    public void finalize ( RSAPrivateCrtKeyParameters key, CObj c )
    {
        //Check private value to see if we want to generate payment
        String skip = c.getPrivate ( CObj.PRV_SKIP_PAYMENT );

        if ( "true".equals ( skip ) )
        {
            c.signX ( key, 0 );
        }

        else
        {
            c.signX ( key, Wrapper.getGenPayment() );
        }

    }

    public boolean check ( RSAKeyParameters key, CObj c )
    {
        String id = c.getString ( CObj.CREATOR );

        if ( id == null )
        {
            return false;
        }

        CObj ident = index.getIdentity ( id );

        if ( ident == null )
        {
            return false;
        }

        return check ( key, ident, c );
    }

    public boolean check ( RSAKeyParameters key, CObj ident, CObj c )
    {
        boolean checkpayment = true;

        //Check if user is trusted by rank
        if ( CObj.SPAMEXCEPTION.equals ( c.getType() ) )
        {
            checkpayment = false;
        }

        Long rnk = ident.getPrivateNumber ( CObj.PRV_USER_RANK );

        if ( rnk != null && rnk > Wrapper.getPaymentRank() )
        {
            checkpayment = false;
        }

        if ( checkpayment )
        {
            //Check if user is trusted by exception
            Long seq = c.getNumber ( CObj.SEQNUM );
            String comid = c.getString ( CObj.COMMUNITYID );
            String id = ident.getId();

            if ( comid != null )
            {
                id = HasFileCreator.getCommunityMemberId ( id, comid );
            }

            CObj spex = index.getById ( EXSPAMPREFIX + id );

            if ( spex != null && seq != null )
            {
                Long v = spex.getNumber ( c.getType() );

                if ( v != null )
                {
                    if ( v >= seq )
                    {
                        checkpayment = false;
                    }

                }

            }

        }

        if ( checkpayment )
        {
            long target = Wrapper.getCheckPayment();

            if ( target == Wrapper.OLDPAYMENT_V0 )
            {
                return c.checkSignatureX_V0 ( key, target );
            }

            return c.checkSignatureX ( key, target );
        }

        return c.checkSignatureX ( key, 0 );
    }

}
