package aktie.index;

import java.io.File;

import aktie.data.CObj;

public class Upgrade0405
{

    public static void upgrade ( String indexdir )
    {
        if ( indexdir != null )
        {
            try
            {
                System.out.println ( "Updating Lucene index user ranks." );
                System.out.println ( "Please be patient." );
                File idxdir = new File ( indexdir );

                Index index = new Index();
                index.setIndexdir ( idxdir );
                index.init();

                CObjList idlst = index.getIdentities();

                for ( int c = 0; c < idlst.size(); c++ )
                {
                    System.out.print ( "." );
                    CObj co = idlst.get ( c );
                    co.pushPrivateNumber ( CObj.PRV_USER_RANK, 5L );
                    index.index ( co );
                    CObjList plst = index.getCreatedBy ( co.getId() );

                    for ( int c1 = 0; c1 < plst.size(); c1++ )
                    {
                        CObj p = plst.get ( c1 );
                        p.pushPrivateNumber ( CObj.PRV_USER_RANK, 5L );
                        index.index ( p );
                    }

                }

                System.out.println();

                Thread.sleep ( Index.MIN_TIME_BETWEEN_SEARCHERS );

                idlst.close();
                index.close();

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

    }


}
