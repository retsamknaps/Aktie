package aktie.upgrade;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import aktie.Node;
import aktie.UpdateCallback;
import aktie.Wrapper;
import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.index.CObjList;
import aktie.utils.FUtils;

public class NodeUpgrader implements UpdateCallback
{

    Logger log = Logger.getLogger ( "aktie" );

    private UpgradeControllerCallback controller;
    private String nodeDir;
    private Node node;

    public NodeUpgrader ( Node n, String nd, UpgradeControllerCallback c )
    {
        node = n;
        nodeDir = nd;
        controller = c;
    }

    @Override
    public void update ( Object o )
    {
        if ( o instanceof CObj )
        {
            final CObj co = ( CObj ) o;

            if ( co.getString ( CObj.ERROR ) == null )
            {
                if ( CObj.HASFILE.equals ( co.getType() ) )
                {

                    checkDownloadUpgrade ( co );
                    checkUpgradeDownloadComplete ( co );
                }

            }

        }

    }

    private void checkDownloadUpgrade ( CObj co )
    {
        String creator = co.getString ( CObj.CREATOR );

        DeveloperIdentity di = null; 
        if (creator != null) {
        	di = node.getDeveloper(creator);
        }
        
        if ( di != null )
        {

            Long createdon = co.getNumber ( CObj.CREATEDON );

            if ( createdon != null && createdon > Wrapper.RELEASETIME )
            {
                String update = co.getString ( CObj.UPGRADEFLAG );
                String fname = co.getString ( CObj.NAME );
                String comid = co.getString ( CObj.COMMUNITYID );
                String stillhasfile = co.getString ( CObj.STILLHASFILE );

                if ( "true".equals ( update ) && "true".equals ( stillhasfile ) )
                {
                    if ( controller.doUpgrade() )
                    {

                        File nodedir = new File ( nodeDir );
                        String parent = nodedir.getParent();

                        if ( parent == null )
                        {
                            parent = nodeDir;
                        }

                        //check current version
                        String libf = parent +
                                      File.separator + "lib" +
                                      File.separator + fname;
                        File cf = new File ( libf );
                        //do upgrade if current digest does not match the upgrade file
                        boolean doup = true;
                        String ndig = co.getString ( CObj.FILEDIGEST );
                        Long flen = co.getNumber ( CObj.FILESIZE );

                        if ( cf.exists() )
                        {
                            String wdig = FUtils.digWholeFile ( libf );
                            doup = !wdig.equals ( ndig );
                        }

                        if ( doup && flen != null )
                        {
                            String updir = parent +
                                           File.separator + "upgrade";
                            File uf = new File ( updir );

                            if ( !uf.exists() )
                            {
                                uf.mkdirs();
                            }

                            String upfile = uf.getPath() +
                                            File.separator + fname;

                            Wrapper.saveUpdateLength ( fname, Long.toString ( flen ) );

                            File f = new File ( upfile );

                            if ( f.exists() ) { f.delete(); }

                            co.pushPrivate ( CObj.LOCALFILE, upfile );
                            co.pushPrivate ( CObj.UPGRADEFLAG, "true" ); //confirm upgrade
                            co.setType ( CObj.USR_DOWNLOAD_FILE );
                            //the user to restart his node.
                            //find a member of this group
                            CObjList mysubs = node.getIndex().getMySubscriptions ( comid );
                            String selid = null;

                            //At least one connected id should be subscribed to
                            //the correct community or we would not have gotten
                            //the update just now
                            List<CObj> conids = node.getConnectionManager().getConnectedIdentities();
                            Set<String> conset = new HashSet<String>();

                            for ( CObj cc : conids )
                            {
                                conset.add ( cc.getId() );
                            }

                            for ( int c = 0; c < mysubs.size() && selid == null; c++ )
                            {
                                try
                                {
                                    CObj ss = mysubs.get ( c );
                                    selid = ss.getString ( CObj.CREATOR );

                                    if ( conset.contains ( selid ) )
                                    {
                                        CObj dl = co.clone();
                                        dl.pushString ( CObj.CREATOR, selid );
                                        node.enqueue ( dl );
                                    }

                                    else
                                    {
                                        selid = null;
                                    }

                                }

                                catch ( Exception e )
                                {
                                    e.printStackTrace();
                                }

                            }

                            mysubs.close();

                            if ( selid != null )
                            {

                                controller.updateMessage ( Wrapper.VERSION + "  Update downloading.." );
                            }

                            else
                            {
                                log.warning ( "No subscription matching community of update" );
                            }

                        }

                    }

                }

            }

        }

    }

    private void checkUpgradeDownloadComplete ( CObj co )
    {
        // *Private* UPGRADEFLAG is set for our own HASFILE once
        //we complete the download.
        String upf = co.getPrivate ( CObj.UPGRADEFLAG );
        String shf = co.getString ( CObj.STILLHASFILE );
        String mine = co.getPrivate ( CObj.MINE );

        if ( "true".equals ( upf ) && "true".equals ( shf ) && "true".equals ( mine ) )
        {
            log.info ( "Upgrade download completed." );
            controller.updateMessage ( Wrapper.VERSION + "   Update downloaded.  Please restart." );

        }

    }



}
