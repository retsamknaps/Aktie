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

    class HashThread implements Runnable
    {

        private CObj tmpCo;
        private HashCallback callBack;
        private RSAPrivateCrtKeyParameters key;

        public HashThread ( CObj c, HashCallback cb, RSAPrivateCrtKeyParameters k )
        {
            tmpCo = c.clone();
            callBack = cb;
            key = k;
            Thread t = new Thread ( this );
            t.start();
        }

        public void stop()
        {
            tmpCo.GiveUp();
        }

        @Override
        public void run()
        {
            tmpCo.signX ( key, Wrapper.getGenPayment() );
            callBack.done ( tmpCo );
        }

    }

    class HashCallback
    {
        private CObj co;

        public synchronized void done ( CObj c )
        {
            co = c;
            notifyAll();
        }

        public synchronized CObj waitForDone()
        {
            if ( co != null )
            {
                return co;
            }

            try
            {
                wait();
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

            return co;
        }

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
            HashCallback hb = new HashCallback();
            int numThreads = Wrapper.getPaymentThreads();
            HashThread ht[] = new HashThread[numThreads];

            for ( int ct = 0; ct < numThreads; ct++ )
            {
                ht[ct] = new HashThread ( c, hb, key );
            }

            CObj doneObj = hb.waitForDone();
            doneObj.makeCopy ( c );

            for ( int ct = 0; ct < numThreads; ct++ )
            {
                ht[ct].stop();
            }

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

            if ( comid != null && !CObj.SUBSCRIPTION.equals ( c.getType() ) )
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
                boolean chkv = c.checkSignatureX_V0 ( key, target );
                return chkv;
            }

            boolean chkv = c.checkSignatureX ( key, target );
            return chkv;
        }

        boolean chkv = c.checkSignatureX ( key, 0 );
        return chkv;
    }

}
