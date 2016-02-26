package aktie.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aktie.Node;
import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.data.RequestFile;
//import aktie.gui.IdentitySubTreeProvider.TreeIdentity;
//import aktie.gui.IdentitySubTreeProvider.TreeSubscription;
import aktie.gui.subtree.SubTreeDragListener;
import aktie.gui.subtree.SubTreeDropListener;
import aktie.gui.subtree.SubTreeEntity;
import aktie.gui.subtree.SubTreeEntityDB;
import aktie.gui.subtree.SubTreeLabelProvider;
import aktie.gui.subtree.SubTreeListener;
import aktie.gui.subtree.SubTreeModel;
import aktie.gui.subtree.SubTreeSorter;
import aktie.i2p.I2PNet;
import aktie.index.CObjList;
import aktie.index.Upgrade0301;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager;
import aktie.net.ConnectionThread;
//import aktie.net.RawNet;
import aktie.user.RequestFileHandler;
import aktie.utils.FUtils;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Table;

import swing2swt.layout.BorderLayout;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ComboViewer;

public class SWTApp
{
    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionCallback concallback = new ConnectionCallback();

    class ConnectionColumnId extends ColumnLabelProvider
    {

        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            CObj id = ct.getEndDestination();

            if ( id != null )
            {
                return id.getDisplayName();
            }

            return "Connecting/Authenticating";
        }

    }

    class ConnectionColumnDest extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;

            if ( ct != null && ct.getLocalDestination() != null &&
                    ct.getLocalDestination().getIdentity() != null )
            {
                return ct.getLocalDestination().getIdentity().getDisplayName();
            }

            return "?";
        }

    }

    class ConnectionColumnLastSent extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            return ct.getLastSent();
        }

    }

    class ConnectionColumnLastRead extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            return ct.getLastRead() + " " + ct.getListCount();
        }

    }

    class ConnectionColumnMode extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;

            if ( ct.isFileMode() )
            {
                return "FILE";
            }

            return "norm";
        }

    }

    class ConnectionColumnPending extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            return Long.toString ( ct.getPendingFileRequests() );
        }

    }

    class ConnectionColumnDownload extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            return Long.toString ( ct.getInBytes() );
        }

    }

    class ConnectionColumnTime extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            long curtime = System.currentTimeMillis();
            ConnectionThread ct = ( ConnectionThread ) element;
            long tm = curtime - ct.getStartTime();
            tm = tm / ( 1000L );
            return Long.toString ( tm );
        }

    }

    class ConnectionColumnUpload extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            ConnectionThread ct = ( ConnectionThread ) element;
            return Long.toString ( ct.getOutBytes() );
        }

    }

    class ConnectionContentProvider implements IStructuredContentProvider
    {
        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
        {
        }

        @Override
        public Object[] getElements ( Object a )
        {
            if ( a instanceof ConnectionCallback )
            {
                ConnectionCallback cc = ( ConnectionCallback ) a;
                return cc.getElements();
            }

            return null;
        }

    }

    class ConnectionSorter extends ViewerSorter
    {
        private int column;

        private boolean reverse;

        public void doSort ( int column )
        {
            if ( column == this.column )
            {
                reverse = !reverse;
            }

            else
            {
                this.column = column;
                reverse = false;
            }

        }

        @SuppressWarnings ( { "rawtypes", "unchecked" } )

        public int compare ( Viewer viewer, Object e1, Object e2 )
        {
            if ( e1 instanceof ConnectionThread &&
                    e2 instanceof ConnectionThread )
            {
                ConnectionThread ct1 = ( ConnectionThread ) e1;
                ConnectionThread ct2 = ( ConnectionThread ) e2;
                ColumnLabelProvider labprov = null;

                int cc = 0;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnDest();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnId();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnUpload();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnDownload();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnTime();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnLastSent();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnLastRead();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnPending();
                }

                cc++;

                if ( column == cc )
                {
                    labprov = new ConnectionColumnMode();
                }

                if ( labprov != null )
                {
                    String s0 = labprov.getText ( ct1 );
                    String s1 = labprov.getText ( ct2 );

                    Comparable dn0 = s0;
                    Comparable dn1 = s1;

                    if ( ( column > 1 && column < 5 ) || column == 7 )
                    {
                        dn0 = Long.valueOf ( s0 );
                        dn1 = Long.valueOf ( s1 );
                    }

                    if ( !reverse )
                    {
                        return dn0.compareTo ( dn1 );
                    }

                    return dn1.compareTo ( dn0 );
                }

            }

            return 0;
        }

    }

    class DownloadContentProvider implements IStructuredContentProvider
    {
        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
        {
        }

        @Override
        public Object[] getElements ( Object a )
        {
            if ( a instanceof RequestFileHandler )
            {
                RequestFileHandler cc = ( RequestFileHandler ) a;
                List<RequestFile> rfl = cc.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );
                Object r[] = new Object[rfl.size()];
                Iterator<RequestFile> i = rfl.iterator();
                int idx = 0;

                while ( i.hasNext() && idx < r.length )
                {
                    r[idx] = i.next();
                    idx++;
                }

                return r;
            }

            return null;
        }

    }

    class DownloadsColumnFileName extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            return rf.getLocalFile();
        }

    }

    class DownloadsColumnPriority extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            return Integer.toString ( rf.getPriority() );
        }

    }

    class DownloadsColumnDownloaded extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            return Long.toString ( rf.getFragsComplete() );
        }

    }

    class DownloadsColumnTotalFragments extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            return Long.toString ( rf.getFragsTotal() );
        }

    }

    class DownloadsColumnFileSize extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            return Long.toString ( rf.getFileSize() );
        }

    }

    class DownloadsColumnState extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;

            if ( rf.getState() == RequestFile.INIT )
            {
                return "init";
            }

            if ( rf.getState() == RequestFile.REQUEST_FRAG )
            {
                return "download parts";
            }

            if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST )
            {
                return "requesting list";
            }

            if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST_SNT )
            {
                return "waiting on list";
            }

            return rf.getState() + "?";
        }

    }

    private Map<String, String> idMap = new HashMap<String, String>();
    class DownloadsColumnId extends ColumnLabelProvider
    {
        @Override
        public String getText ( Object element )
        {
            RequestFile rf = ( RequestFile ) element;
            String rid = rf.getRequestId();
            String dn = idMap.get ( rid );

            if ( dn == null )
            {
                dn = "";

                if ( rid != null )
                {
                    CObj myid = getNode().getIndex().getMyIdentity ( rid );

                    if ( myid != null )
                    {
                        dn = myid.getDisplayName();
                    }

                    idMap.put ( rid, dn );
                }

            }

            return dn;
        }

    }

    class DownloadsSorter extends ViewerSorter
    {
        private int column;

        private boolean reverse;

        public void doSort ( int column )
        {
            if ( column == this.column )
            {
                reverse = !reverse;
            }

            else
            {
                this.column = column;
                reverse = false;
            }

        }

        @SuppressWarnings ( { "rawtypes", "unchecked" } )

        public int compare ( Viewer viewer, Object e1, Object e2 )
        {
            if ( e1 instanceof RequestFile &&
                    e2 instanceof RequestFile )
            {
                RequestFile ct1 = ( RequestFile ) e1;
                RequestFile ct2 = ( RequestFile ) e2;
                ColumnLabelProvider labprov = null;

                if ( column == 0 )
                {
                    labprov = new DownloadsColumnFileName();
                }

                if ( column == 1 )
                {
                    labprov = new DownloadsColumnPriority();
                }

                if ( column == 2 )
                {
                    labprov = new DownloadsColumnDownloaded();
                }

                if ( column == 3 )
                {
                    labprov = new DownloadsColumnTotalFragments();
                }

                if ( column == 4 )
                {
                    labprov = new DownloadsColumnFileSize();
                }

                if ( column == 5 )
                {
                    labprov = new DownloadsColumnState();
                }

                if ( column == 6 )
                {
                    labprov = new DownloadsColumnId();
                }

                if ( labprov != null )
                {

                    String s0 = labprov.getText ( ct1 );
                    String s1 = labprov.getText ( ct2 );

                    Comparable dn0 = s0;
                    Comparable dn1 = s1;

                    if ( column > 0 && column < 5 )
                    {
                        dn0 = Long.valueOf ( s0 );
                        dn1 = Long.valueOf ( s1 );
                    }

                    if ( !reverse )
                    {
                        return dn0.compareTo ( dn1 );
                    }

                    return dn1.compareTo ( dn0 );
                }

            }

            return 0;
        }

    }

    public static long UPDATE_INTERVAL = 1000;

    class ConnectionCallback implements ConnectionListener
    {
        public Set<ConnectionThread> connections = new HashSet<ConnectionThread>();
        private long lastDisplay = 0;
        private void updateDisplay ( boolean force )
        {
            long curtime = System.currentTimeMillis();

            if ( curtime > ( lastDisplay + UPDATE_INTERVAL ) || force )
            {
                lastDisplay = curtime;
                final ConnectionCallback This = this;
                Display.getDefault().asyncExec ( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        connectionTableViewer.setInput ( This );
                    }

                } );

            }

        }

        @Override
        public void update ( ConnectionThread ct )
        {
            if ( !ct.isStopped() )
            {
                synchronized ( connections )
                {
                    connections.add ( ct );
                }

                updateDisplay ( false );
            }

            else
            {
                closed ( ct );
            }

        }

        @Override
        public void closed ( ConnectionThread ct )
        {
            synchronized ( connections )
            {
                connections.remove ( ct );
            }

            updateDisplay ( true );
        }

        public Object[] getElements()
        {
            Object r[] = null;

            synchronized ( connections )
            {
                r = new Object[connections.size()];
                Iterator<ConnectionThread> i = connections.iterator();
                int idx = 0;

                while ( i.hasNext() )
                {
                    r[idx] = i.next();
                    idx++;
                }

            }

            return r;
        }

    }

    private NetCallback netcallback = new NetCallback();

    /*
        ========================================================================
        this dumb  why would you do this here
        ========================================================================
    */
    private void checkDownloadUpgrade ( CObj co )
    {
        String creator = co.getString ( CObj.CREATOR );

        if ( developerIdentity != null && creator != null &&
                creator.equals ( developerIdentity.getId() ) )
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
                    if ( doUpgrade )
                    {

                        File nodedir = new File ( nodeDir );
                        String parent = nodedir.getParent();

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
                            String upfile = parent +
                                            File.separator + "upgrade" +
                                            File.separator + fname;

                            Wrapper.saveUpdateLength ( fname, Long.toString ( flen ) );

                            File f = new File ( upfile );

                            if ( f.exists() ) { f.delete(); }

                            co.pushPrivate ( CObj.LOCALFILE, upfile );
                            co.pushPrivate ( CObj.UPGRADEFLAG, "true" ); //confirm upgrade
                            co.setType ( CObj.USR_DOWNLOAD_FILE );
                            //the user to restart his node.
                            //find a member of this group
                            CObjList mysubs = getNode().getIndex().getMySubscriptions ( comid );
                            String selid = null;

                            for ( int c = 0; c < mysubs.size() && selid == null; c++ )
                            {
                                try
                                {
                                    CObj ss = mysubs.get ( c );
                                    selid = ss.getString ( CObj.CREATOR );
                                    CObj dl = co.clone();
                                    dl.pushString ( CObj.CREATOR, selid );
                                    node.enqueue ( dl );
                                }

                                catch ( Exception e )
                                {
                                    e.printStackTrace();
                                }

                            }

                            mysubs.close();

                            if ( selid != null )
                            {

                                Display.getDefault().asyncExec ( new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        lblVersion.setText ( Wrapper.VERSION + "  Update downloading.." );
                                    }

                                } );

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

        if ( "true".equals ( upf ) && "true".equals ( shf ) )
        {
            log.info ( "Upgrade download completed." );
            Display.getDefault().asyncExec ( new Runnable()
            {
                @Override
                public void run()
                {
                    lblVersion.setText ( Wrapper.VERSION + "   Update downloaded.  Please restart." );
                }

            } );

        }

    }

    private void updateBanner ( CObj co )
    {
        String creator = co.getString ( CObj.CREATOR );

        if ( developerIdentity != null && creator != null &&
                creator.equals ( developerIdentity.getId() ) )
        {

            //Update subject line
            final String subj = co.getString ( CObj.SUBJECT );

            if ( subj != null )
            {
                Wrapper.saveLastDevMessage ( subj );

                Display.getDefault().asyncExec ( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if ( bannerText != null && !bannerText.isDisposed() )
                        {
                            bannerText.setText ( subj );
                        }

                    }

                } );

            }

        }

    }

    class NetCallback implements GuiCallback
    {
        @Override
        public void update ( Object o )
        {
            if ( o instanceof RequestFile )
            {
                if ( downloadTableViewer != null )
                {
                    Display.getDefault().asyncExec ( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            downloadTableViewer.setInput ( getNode().getFileHandler() );
                        }

                    } );

                }

            }

            if ( o instanceof CObj )
            {
                if ( o != null )
                {
                    final CObj co = ( ( CObj ) o ).clone();

                    if ( co.getString ( CObj.ERROR ) != null )
                    {
                        boolean clear = true;
                        String dclear = co.getPrivate ( CObj.PRV_CLEAR_ERR );

                        if ( "false".equals ( dclear ) )
                        {
                            clear = false;
                        }

                        setErrorMessage ( co.getString ( CObj.ERROR ), clear );
                    }

                    else
                    {
                        String type = co.getType();
                        String comid = co.getString ( CObj.COMMUNITYID );

                        if ( CObj.POST.equals ( type ) )
                        {

                            updateBanner ( co );

                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    addData ( co );
                                }

                            } );

                            if ( selectedCommunity != null && comid != null && comid.equals ( selectedCommunity.getDig() ) )
                            {

                                Display.getDefault().asyncExec ( new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        postSearch();
                                    }

                                } );

                            }

                        }

                        if ( CObj.MEMBERSHIP.equals ( type ) || CObj.COMMUNITY.equals ( type ) )
                        {
                            if ( "true".equals ( co.getPrivate ( CObj.MINE ) ) )
                            {
                                Display.getDefault().asyncExec ( new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        updateMembership();
                                    }

                                } );

                            }

                        }

                        if ( CObj.HASFILE.equals ( type ) )
                        {
                            if ( selectedCommunity != null && comid != null && comid.equals ( selectedCommunity.getDig() ) )
                            {
                                Display.getDefault().asyncExec ( new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        filesSearch();
                                        updateShareCount();
                                    }

                                } );

                            }

                            checkUpgradeDownloadComplete ( co );
                            checkDownloadUpgrade ( co );

                        }

                    }

                }

            }

        }

    }

    public UsrCallback getUserCallback()
    {
        return usrcallback;
    }

    private UsrCallback usrcallback = new UsrCallback();

    class UsrCallback implements GuiCallback
    {
        @Override
        public void update ( Object o )
        {
            if ( o instanceof RequestFile )
            {
                if ( downloadTableViewer != null )
                {
                    Display.getDefault().asyncExec ( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            downloadTableViewer.setInput ( getNode().getFileHandler() );
                        }

                    } );

                }

            }

            if ( o instanceof CObj )
            {
                final CObj co = ( CObj ) o;

                if ( co.getString ( CObj.ERROR ) != null )
                {
                    boolean clear = true;
                    String dclear = co.getPrivate ( CObj.PRV_CLEAR_ERR );

                    if ( "false".equals ( dclear ) )
                    {
                        clear = false;
                    }

                    setErrorMessage ( co.getString ( CObj.ERROR ), clear );
                }

                else
                {
                    setErrorMessage ( "", false );
                    String comid = co.getString ( CObj.COMMUNITYID );

                    if ( CObj.FILE.equals ( co.getType() ) )
                    {
                        Display.getDefault().asyncExec ( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                filesSearch();
                            }

                        } );

                    }

                    if ( CObj.IDENTITY.equals ( co.getType() ) )
                    {
                        final String name = co.getDisplayName();

                        if ( identTreeViewer != null && name != null )
                        {
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    addData ( co );
                                }

                            } );

                        }

                    }

                    if ( CObj.COMMUNITY.equals ( co.getType() ) )
                    {
                        CObj sub = new CObj();
                        sub.setType ( CObj.SUBSCRIPTION );
                        sub.pushString ( CObj.CREATOR, co.getString ( CObj.CREATOR ) );
                        sub.pushString ( CObj.COMMUNITYID, co.getDig() );
                        sub.pushString ( CObj.SUBSCRIBED, "true" );
                        getNode().enqueue ( sub );
                    }

                    if ( CObj.SUBSCRIPTION.equals ( co.getType() ) )
                    {
                        final String creatorid = co.getString ( CObj.CREATOR );

                        if ( creatorid != null && comid != null )
                        {
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    addData ( co );
                                }

                            } );

                        }

                    }

                    if ( CObj.POST.equals ( co.getType() ) )
                    {

                        updateBanner ( co );

                        Display.getDefault().asyncExec ( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                addData ( co );
                            }

                        } );

                        if ( selectedCommunity != null && comid != null && comid.equals ( selectedCommunity.getDig() ) )
                        {
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    postSearch();
                                }

                            } );

                        }

                    }

                    if ( CObj.MEMBERSHIP.equals ( co.getType() ) ||
                            CObj.COMMUNITY.equals ( co.getType() ) )
                    {
                        if ( "true".equals ( co.getPrivate ( CObj.MINE ) ) )
                        {
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    updateMembership();
                                }

                            } );

                        }

                    }

                    if ( CObj.HASFILE.equals ( co.getType() ) )
                    {

                        checkPendingPosts ( co );
                        checkDownloadUpgrade ( co );
                        checkUpgradeDownloadComplete ( co );

                        if ( selectedCommunity != null && comid != null && comid.equals ( selectedCommunity.getDig() ) )
                        {
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    filesSearch();
                                    updateShareCount();
                                }

                            } );

                        }

                    }

                }

            }

        }

    }

    class SaveSeeds implements SelectionListener
    {
        public void widgetSelected ( SelectionEvent event )
        {
            FileDialog fd = new FileDialog ( shell, SWT.SAVE );
            fd.setText ( "Save" );
            //fd.setFilterPath();
            String[] filterExt = { "*" };

            fd.setFilterExtensions ( filterExt );
            String selected = fd.open();

            if ( node != null && selected != null )
            {
                try
                {
                    PrintWriter pw = new PrintWriter ( new FileOutputStream ( new File ( selected ) ) );
                    CObjList ilst = node.getIndex().getIdentities();

                    for ( int c = 0; c < ilst.size(); c++ )
                    {
                        CObj i = ilst.get ( c );
                        JSONObject jo = i.getJSON();
                        jo.write ( pw );
                        pw.println();
                    }

                    ilst.close();
                    pw.close();
                }

                catch ( Exception e )
                {

                }

            }

        }

        public void widgetDefaultSelected ( SelectionEvent event )
        {
        }

    }

    class LoadSeeds implements SelectionListener
    {
        public void widgetSelected ( SelectionEvent event )
        {
            FileDialog fd = new FileDialog ( shell, SWT.OPEN );
            fd.setText ( "Open" );
            //fd.setFilterPath();
            String[] filterExt = { "*.*" };

            fd.setFilterExtensions ( filterExt );
            String selected = fd.open();

            if ( selected != null && node != null )
            {
                loadSeed ( new File ( selected ) );
            }

        }

        public void widgetDefaultSelected ( SelectionEvent event )
        {
        }

    }

    class AddFile implements SelectionListener
    {
        @Override
        public void widgetSelected ( SelectionEvent evt )
        {
            if ( selectedCommunity != null && selectedIdentity != null )
            {
                FileDialog fd = new FileDialog ( shell, SWT.OPEN | SWT.MULTI );
                fd.setText ( "Add File" );
                //fd.setFilterPath();
                String[] filterExt =
                {
                    "*",
                    "*.txt",
                    "*.pdf",
                    "*.exe",
                    "*.jpg",
                    "*.jpeg",
                    "*.png",
                    "*.gif",
                    "*.bmp",
                    "*.mov",
                    "*.mpg",
                    "*.mpeg",
                    "*.avi",
                    "*.flv",
                    "*.wmv",
                    "*.webv",
                    "*.rm"
                };

                fd.setFilterExtensions ( filterExt );
                fd.open();
                String selary[] = fd.getFileNames();
                String selpath = fd.getFilterPath();

                for ( int c = 0; c < selary.length; c++ )
                {
                    File f = new File ( selpath + File.separator + selary[c] );

                    if ( f.exists() )
                    {
                        if ( f.isFile() )
                        {

                            boolean isupgrade = false;

                            if ( developerIdentity != null )
                            {
                                if ( developerIdentity.getId().equals ( selectedIdentity.getId() ) )
                                {
                                    isupgrade = MessageDialog.openConfirm ( shell, "Update", "Are you sure you want this to be an update file?" );
                                }

                            }

                            CObj nf = new CObj();
                            nf.setType ( CObj.HASFILE );
                            nf.pushString ( CObj.COMMUNITYID, selectedCommunity.getDig() );
                            nf.pushString ( CObj.CREATOR, selectedIdentity.getId() );
                            nf.pushPrivate ( CObj.LOCALFILE, f.getPath() );

                            if ( isupgrade )
                            {
                                nf.pushString ( CObj.UPGRADEFLAG, "true" );
                                //Set private value too so that we say we have it for ourself.
                                nf.pushPrivate ( CObj.UPGRADEFLAG, "true" );
                            }

                            node.enqueue ( nf );
                        }

                    }

                }

            }

            else
            {
                MessageDialog.openWarning ( shell, "Select a community.", "Sorry, you have to select the community you wish to add a file to." );
            }

        }

        @Override
        public void widgetDefaultSelected ( SelectionEvent e )
        {
        }

    }

    private void updateCommunity ( CObj cm )
    {
        if ( cm != null && cm.getDig() != null )
        {
            CObj u = new CObj();
            u.setType ( CObj.USR_HASFILE_UPDATE );
            u.pushString ( CObj.COMMUNITYID, cm.getDig() );
            getNode().enqueue ( u );
            u = new CObj();
            u.setType ( CObj.USR_POST_UPDATE );
            u.pushString ( CObj.COMMUNITYID, cm.getDig() );
            getNode().enqueue ( u );
            getNode().sendRequestsNow();
        }

    }

    private void updateAll()
    {
        CObj u = new CObj();
        u.setType ( CObj.USR_IDENTITY_UPDATE );
        getNode().enqueue ( u );
        u = new CObj();
        u.setType ( CObj.USR_COMMUNITY_UPDATE );
        getNode().enqueue ( u );
        u = new CObj();
        u.setType ( CObj.USR_MEMBER_UPDATE );
        getNode().enqueue ( u );
        u = new CObj();
        u.setType ( CObj.USR_SUB_UPDATE );
        getNode().enqueue ( u );
        u = new CObj();
        u.setType ( CObj.USR_HASFILE_UPDATE );
        getNode().enqueue ( u );
        u = new CObj();
        u.setType ( CObj.USR_POST_UPDATE );
        getNode().enqueue ( u );
        getNode().sendRequestsNow();
    }

    class ManualUpdate implements SelectionListener
    {
        @Override
        public void widgetSelected ( SelectionEvent e )
        {
            updateAll();
        }

        @Override
        public void widgetDefaultSelected ( SelectionEvent e )
        {
        }

    }

    private SWTSplash splash;
    private Label lblVersion;
    private boolean doUpgrade = true;
    protected Shell shell;
    private Text searchText;
    private Tree identTree;
    private TreeViewer identTreeViewer;
    private NewCommunityDialog newCommunityDialog;
    private ShowHasFileDialog hasFileDialog;
    private NewIdentityDialog newIdentityDialog;
    private SubscriptionDialog subscriptionDialog;
    private NewMemberDialog newMemberDialog;
    private NewPostDialog newPostDialog;
    private DownloadPriorityDialog downloadPriorityDialog;
    private ShowPrivComDialog privComDialog;
    private ShowMembersDialog membersDialog;
    private NewDirectoryShareDialog shareDialog;
    private DownloadToShareDialog downloadToShareDialog;
    private I2PSettingsDialog i2pDialog;
    private AddFolderDialog addFolderDialog;
    private AdvancedSearchDialog advancedDialog;
    //private IdentitySubTreeModel identSubTreeModel;
    private SubTreeModel identModel;

    private Node node;
    private String nodeDir;

    private CObj selectedIdentity;
    private CObj selectedCommunity;
    private Label lblIdentCommunity;
    private Table postTable;
    private TableViewer postTableViewer;
    private CObjListContentProvider postContentProvider;
    private CObjListContentProvider fileContentProvider;
    private CObj displayedPost;
    private StyledText postText;
    private Table connectionTable;
    private TableViewer connectionTableViewer;
    private Text fileSearch;
    private Table fileTable;
    private TableViewer fileTableViewer;
    private Table downloadTable;
    private TableViewer downloadTableViewer;
    private String exportCommunitiesFile;
    private CObj developerIdentity;
    private Map<String, List<CObj>> pendingPosts = new HashMap<String, List<CObj>>();
    private CObj advQuery;

    //Should be a HASFILE
    private void checkPendingPosts ( CObj c )
    {
        String lfname = c.getPrivate ( CObj.LOCALFILE );

        if ( lfname != null )
        {
            synchronized ( pendingPosts )
            {
                List<CObj> pplst = pendingPosts.get ( lfname );

                if ( pplst != null )
                {
                    Iterator<CObj> i = pplst.iterator();

                    while ( i.hasNext() )
                    {
                        CObj pst = i.next();
                        String fname = pst.getPrivate ( CObj.LOCALFILE );
                        String pvname = pst.getPrivate ( CObj.PRV_LOCALFILE );

                        //Primary file attachment
                        if ( lfname.equals ( fname ) )
                        {
                            pst.pushString ( CObj.NAME,       c.getString ( CObj.NAME ) );
                            pst.pushNumber ( CObj.FILESIZE,   c.getNumber ( CObj.FILESIZE ) );
                            pst.pushString ( CObj.FRAGDIGEST, c.getString ( CObj.FRAGDIGEST ) );
                            pst.pushNumber ( CObj.FRAGSIZE,   c.getNumber ( CObj.FRAGSIZE ) );
                            pst.pushNumber ( CObj.FRAGNUMBER, c.getNumber ( CObj.FRAGNUMBER ) );
                            pst.pushString ( CObj.FILEDIGEST, c.getString ( CObj.FILEDIGEST ) );
                        }

                        if ( lfname.equals ( pvname ) )
                        {
                            pst.pushString ( CObj.PRV_NAME,       c.getString ( CObj.NAME ) );
                            pst.pushNumber ( CObj.PRV_FILESIZE,   c.getNumber ( CObj.FILESIZE ) );
                            pst.pushString ( CObj.PRV_FRAGDIGEST, c.getString ( CObj.FRAGDIGEST ) );
                            pst.pushNumber ( CObj.PRV_FRAGSIZE,   c.getNumber ( CObj.FRAGSIZE ) );
                            pst.pushNumber ( CObj.PRV_FRAGNUMBER, c.getNumber ( CObj.FRAGNUMBER ) );
                            pst.pushString ( CObj.PRV_FILEDIGEST, c.getString ( CObj.FILEDIGEST ) );
                        }

                        if ( ( fname == null ||
                                ( fname != null && pst.getString ( CObj.FILEDIGEST ) != null ) ) &&
                                ( pvname == null ||
                                  ( pvname != null && pst.getString ( CObj.PRV_FILEDIGEST ) != null ) ) )
                        {
                            i.remove();
                            getNode().enqueue ( pst );
                        }

                    }

                    if ( pplst.size() == 0 )
                    {
                        pendingPosts.remove ( lfname );
                    }

                }

            }

        }

    }

    public void addPendingPost ( CObj c )
    {
        String fname = c.getPrivate ( CObj.LOCALFILE );
        String pvname = c.getPrivate ( CObj.PRV_LOCALFILE );

        if ( fname != null )
        {
            synchronized ( pendingPosts )
            {
                List<CObj> pl = pendingPosts.get ( fname );

                if ( pl == null )
                {
                    pl = new LinkedList<CObj>();
                    pendingPosts.put ( fname, pl );
                }

                pl.add ( c );
            }

        }

        if ( pvname != null )
        {
            synchronized ( pendingPosts )
            {
                List<CObj> pl = pendingPosts.get ( pvname );

                if ( pl == null )
                {
                    pl = new LinkedList<CObj>();
                    pendingPosts.put ( pvname, pl );
                }

                pl.add ( c );
            }

        }

    }

    public CObj getSelectedCommunity()
    {
        return selectedCommunity;
    }

    public Node getNode()
    {
        return node;
    }

    public void setSelected ( CObj id, CObj comid )
    {
        selectedIdentity = id;
        selectedCommunity = comid;
        lblIdentCommunity.setText ( "Identity: " + selectedIdentity.getDisplayName() +
                                    "  Community: " + selectedCommunity.getPrivateDisplayName() );

        //TODO: Do this again identSubTreeModel.clearNew ( comid );
        identModel.clearBlue ( comid );
        identTreeViewer.refresh ( true );

        setShares ( comid.getDig(), id.getId() );

        searchText.setText ( "" );
        fileSearch.setText ( "" );
        advQuery = null;
        postSearch ( );
        filesSearch ( "" );
        postText.setText ( "" );
    }

    public void setVerbose()
    {
        System.out.println ( "SETTING VERBOSE-!-" );
        log.setLevel ( Level.INFO );
    }

    public void setSevere()
    {
        log.setLevel ( Level.SEVERE );
    }

    /**
        Launch the application.
        @param args
    */
    public static void main ( String[] args )
    {
        boolean verbose = false;

        try
        {

            SWTApp window = new SWTApp();

            if ( args.length > 0 )
            {
                window.nodeDir = args[0];

                if ( args.length > 1 )
                {
                    if ( "-v".equals ( args[1] ) )
                    {
                        verbose = true;
                    }

                    else
                    {
                        window.exportCommunitiesFile = args[1];
                    }

                }

                if ( args.length > 2 )
                {
                    for ( int ct = 2; ct < args.length && !verbose; ct++ )
                    {
                        verbose = "-v".equals ( args[ct] );
                    }

                }

            }

            else
            {
                window.nodeDir = "aktie_node";
            }

            if ( verbose )
            {
                window.setVerbose();
            }

            else
            {
                window.setSevere();
            }

            window.open();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        System.exit ( 0 );

    }

    public void setI2PProps ( Properties p )
    {
        if ( i2pnet != null )
        {
            i2pnet.setProperties ( p );
            List<CObj> myonlist = new LinkedList<CObj>();
            CObjList myids = node.getIndex().getMyIdentities();

            for ( int c = 0; c < myids.size(); c++ )
            {
                try
                {
                    CObj ido = myids.get ( c );

                    if ( ido.getPrivateNumber ( CObj.PRV_DEST_OPEN ) == null ||
                            ido.getPrivateNumber ( CObj.PRV_DEST_OPEN ) == 1L )
                    {
                        myonlist.add ( ido );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            myids.close();

            for ( CObj mid : myonlist )
            {
                CObj off = mid.clone();
                off.setType ( CObj.USR_START_DEST );
                off.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 0L );
                node.enqueue ( off );
            }

            for ( CObj mid : myonlist )
            {
                CObj on = mid.clone();
                on.setType ( CObj.USR_START_DEST );
                on.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 1L );
                node.enqueue ( on );
            }

        }

    }

    private Properties getI2PReady()
    {
        File i2pp = new File ( nodeDir + File.separator + "i2p.props" );
        i2pDialog = new I2PSettingsDialog ( shell, this, i2pp );
        i2pDialog.create();

        return i2pDialog.getI2PProps();
    }

    private IdentityCache idCache;
    public IdentityCache getIdCache()
    {
        return idCache;
    }

    private void startNodeThread ( final Properties p )
    {
        Thread t = new Thread ( new Runnable()
        {
            public void run()
            {
                i2pnet = new I2PNet ( nodeDir, p );
                i2pnet.waitUntilReady();

                try
                {
                    node = new Node ( nodeDir, i2pnet, usrcallback,
                                      netcallback, concallback );

                    idCache = new IdentityCache ( node.getIndex() );

                    Display.getDefault().asyncExec ( new Runnable()
                    {
                        public void run()
                        {
                            startedSuccessfully();
                        }

                    } );

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    failedToStart();
                }

            }

        }, "Node start thread" );

        t.start();
    }

    public SubTreeModel getIdentModel()
    {
        return identModel;
    }

    public void addFolder ( SubTreeEntity e, String name )
    {
        identModel.addFolder ( e, name );
        identTreeViewer.setInput ( "folder added" );
        identModel.setCollaspseState ( identTreeViewer );
    }

    private void startedSuccessfully()
    {

        //identSubTreeModel = new IdentitySubTreeModel ( this );
        //identTreeViewer.setContentProvider ( new IdentitySubTreeProvider() );
        //
        identModel = new SubTreeModel ( node.getIndex(),
                                        new SubTreeEntityDB ( node.getSession() ) );
        identModel.init();
        identTreeViewer.setContentProvider ( identModel );
        SubTreeListener stl = new SubTreeListener ( identModel );
        identTreeViewer.addTreeListener ( stl );
        //identTreeViewer.setLabelProvider();
        identTreeViewer.setSorter ( new SubTreeSorter() );
        int operations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[] {TextTransfer.getInstance() };

        identTreeViewer.addDragSupport ( operations, transferTypes ,
                                         new SubTreeDragListener ( identTreeViewer ) );
        identTreeViewer.addDropSupport ( operations, transferTypes,
                                         new SubTreeDropListener ( identTreeViewer, identModel ) );

        TreeViewerColumn tvc1 = new TreeViewerColumn ( identTreeViewer, SWT.NONE );
        tvc1.getColumn().setText ( "Name" ); //$NON-NLS-1$
        tvc1.getColumn().setWidth ( 200 );
        tvc1.setLabelProvider ( new DelegatingStyledCellLabelProvider (
                                    new SubTreeLabelProvider ( identModel ) ) );
        //tvc1.setLabelProvider ( new DelegatingStyledCellLabelProvider (
        //                            new IdentitySubTreeLabelProvider() ) );

        try
        {
            CObjList mlst = node.getIndex().getMyIdentities();

            if ( mlst.size() == 0 )
            {
                CObj co = new CObj();
                co.setType ( CObj.IDENTITY );
                co.pushString ( CObj.NAME, "anon" );
                node.enqueue ( co );
                //Load default seed file.
                File defseedfile = new File ( nodeDir + File.separator + "defseed.dat" );

                if ( defseedfile.exists() )
                {
                    loadSeed ( defseedfile );
                }

                //Load default communities and subscribe.
                File defcomfile = new File ( nodeDir + File.separator + "defcom.dat" );

                if ( defcomfile.exists() )
                {
                    loadDefCommunitySubs ( defcomfile );
                }


            }

            else
            {
                for ( int c = 0; c < mlst.size(); c++ )
                {
                    usrcallback.update ( mlst.get ( c ) );
                }

            }

            mlst.close();

            File devid = new File ( nodeDir + File.separator + "developerid.dat" );

            if ( devid.exists() )
            {
                loadDeveloperIdentity ( devid );
            }


            mlst = node.getIndex().getMySubscriptions();

            for ( int c = 0; c < mlst.size(); c++ )
            {
                usrcallback.update ( mlst.get ( c ) );
            }

            mlst.close();

            if ( Wrapper.getStartDestinationsOnStartup() )
            {
                node.startDestinations ( Wrapper.getStartDestinationDelay() );
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        createDialogs();
        exportCommunities();

        startUpdateTimer();
        saveVersionFile();
        startNetUpdateStatusTimer();

    }

    private void failedToStart()
    {
        System.exit ( 1 );
    }


    private I2PNet i2pnet;

    private void startNode()
    {

        String lastversion = lastVersion();

        if ( lastversion != null )
        {
            upgrade0301 ( lastversion );
        }

        // new RawNet ( new File ( nodeDir ) )
        Properties p = getI2PReady();

        startNodeThread ( p );

        if ( lastversion != null )
        {
            upgrade0115 ( lastversion );
        }


    }

    private boolean pendingClear = false;

    private void setErrorMessage ( final String msg, final boolean clear )
    {
        Display.getDefault().asyncExec ( new Runnable()
        {
            @Override
            public void run()
            {
                lblError.setText ( msg );
                composite_header.layout();

                if ( !pendingClear && clear )
                {
                    pendingClear = true;
                    Timer t = new Timer ( "Error message clear", true );
                    t.schedule ( new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            pendingClear = false;
                            Display.getDefault().asyncExec ( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    lblError.setText ( "" );
                                    composite_header.layout();
                                }

                            } );

                        }

                    }, 5L * 60L * 1000L );

                }

            }

        } );

    }

    private void startUpdateTimer()
    {
        Timer t = new Timer ( "Update timer", true );
        t.schedule ( new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    updateAll();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            //Make the update period shorter than the max time with no requests, so that we
            //actually send requests before the time is up.  That way we don't loose the connection
            //when it is actually useful to us.
        }, 0, Math.min ( 30L * 60L * 1000L, ConnectionManager.MAX_TIME_WITH_NO_REQUESTS - ( 10L * 60L * 1000L ) ) );

    }

    private void setVersionNetStatus()
    {
        String netstat = "";

        if ( getNode() != null )
        {
            if ( getNode().getNet() != null )
            {
                netstat = getNode().getNet().getStatus();
            }

        }

        lblVersion.setText ( Wrapper.VERSION + " (" + netstat + ")" );
    }

    private void startNetUpdateStatusTimer()
    {
        Timer t = new Timer ( "I2P Update timer", true );
        t.schedule ( new TimerTask()
        {
            @Override
            public void run()
            {

                Display.getDefault().asyncExec ( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setVersionNetStatus();
                        composite_header.layout();
                    }

                } );

            }

        }, 0, 5L * 60L * 1000L );

    }


    public void closeNode()
    {
        node.close();

        if ( i2pnet != null )
        {
            i2pnet.exit();
        }

    }

    public String lastVersion()
    {
        File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );

        if ( vf.exists() )
        {
            try
            {
                FileReader fr = new FileReader ( vf );
                BufferedReader br = new BufferedReader ( fr );
                String vl = br.readLine();
                br.close();

                return vl;
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return null;
    }

    private void upgrade0115 ( String lastversion )
    {
        if ( Wrapper.compareVersions ( lastversion, Wrapper.VERSION_0115 ) < 0 )
        {
            node.getHasFileCreator().updateHasFile();
        }

    }

    private void upgrade0301 ( String lastversion )
    {
        if ( Wrapper.compareVersions ( lastversion, Wrapper.VERSION_0403 ) < 0 )
        {
            Upgrade0301.upgrade ( nodeDir + File.separator + "index" );
        }

    }

    private boolean isSameOrNewer()
    {
        String vl = lastVersion();

        if ( Wrapper.compareVersions ( vl, Wrapper.VERSION ) > 0 )
        {
            return false;
        }

        return true;
    }

    private void deleteVersionAndExit()
    {
        File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );

        if ( vf.exists() )
        {
            vf.delete();
        }

        System.exit ( Wrapper.RESTART_RC );
    }

    private void saveVersionFile()
    {
        try
        {
            File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );
            FileOutputStream fos = new FileOutputStream ( vf );
            PrintWriter pw = new PrintWriter ( fos );
            pw.println ( Wrapper.VERSION );
            pw.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    /**
        Open the window.
    */
    public void open()
    {
        if ( !isSameOrNewer() )
        {
            //Something went wrong on upgrade
            deleteVersionAndExit();
        }

        Display.setAppName ( "aktie" );
        Display display = Display.getDefault();

        createContents();
        shell.open();
        shell.layout();

        splash = new SWTSplash ( shell );
        startNode();
        splash.open();

        while ( !shell.isDisposed() )
        {
            if ( !display.readAndDispatch() )
            {
                display.sleep();
            }

        }

        System.out.println ( "CLOSING NODE" );
        closeNode();
    }

    private void addData ( CObj co )
    {
        SubTreeModel prov = ( SubTreeModel ) identTreeViewer.getContentProvider();
        prov.update ( co );
        identTreeViewer.setInput ( "Here is some data" );
        identModel.setCollaspseState ( identTreeViewer );

        splash.reallyClose();

    }

    private String sortPostField1;
    private String sortPostField2;
    private boolean sortPostReverse;
    private SortField.Type sortPostType1;
    private SortField.Type sortPostType2;

    private void postSearch ( )
    {
        String srch = searchText.getText();

        if ( selectedCommunity != null )
        {
            CObjList oldlst = ( CObjList ) postTableViewer.getInput();
            Sort s = new Sort();

            if ( sortPostField1 != null )
            {
                if ( sortPostField2 == null )
                {
                    if ( SortedNumericSortField.Type.LONG.equals ( sortPostType1 ) )
                    {
                        s.setSort ( new SortedNumericSortField ( sortPostField1, sortPostType1, sortPostReverse ) );
                    }

                    else
                    {
                        s.setSort ( new SortField ( sortPostField1, sortPostType1, sortPostReverse ) );
                    }

                }

                else
                {
                    SortField sf1 = null;
                    SortField sf2 = null;

                    if ( SortedNumericSortField.Type.LONG.equals ( sortPostType1 ) )
                    {
                        sf1 = new SortedNumericSortField ( sortPostField1, sortPostType1, sortPostReverse );
                    }

                    else
                    {
                        sf1 = new SortField ( sortPostField1, sortPostType1, sortPostReverse );
                    }

                    if ( SortedNumericSortField.Type.LONG.equals ( sortPostType2 ) )
                    {
                        sf2 = new SortedNumericSortField ( sortPostField2, sortPostType2, sortPostReverse );
                    }

                    else
                    {
                        sf2 = new SortField ( sortPostField2, sortPostType2, sortPostReverse );
                    }

                    s.setSort ( sf1, sf2 );
                }

            }

            else
            {
                s.setSort ( new SortedNumericSortField ( CObj.docNumber ( CObj.CREATEDON ), SortedNumericSortField.Type.LONG, true ) );
            }

            if ( advQuery == null )
            {
                CObjList clst = getNode().getIndex().searchPosts ( selectedCommunity.getDig(), srch, s );
                postTableViewer.setInput ( clst );
            }

            else
            {
                List<CObj> ql = new LinkedList<CObj>();
                ql.add ( advQuery );
                CObjList clst = getNode().getIndex().searchPostsQuery ( ql, s );
                postTableViewer.setInput ( clst );
            }

            if ( oldlst != null )
            {
                oldlst.close();
            }

        }

    }

    private void filesSearch()
    {
        String srch = fileSearch.getText();
        filesSearch ( srch );
    }

    private void setShares ( String comid, String memid )
    {
        List<DirectoryShare> lst = getNode().getShareManager().listShares ( comid, memid );
        downloadToShareDialog.setShares ( lst );
        List<DirectoryShare> plst = new LinkedList<DirectoryShare>();
        DirectoryShare alls = new DirectoryShare();
        alls.setId ( -1 );
        alls.setShareName ( "All" );
        plst.add ( alls );
        plst.addAll ( lst );
        comboShareNameViewer.setInput ( plst );
        shareComboViewer.setInput ( lst );
        textShareName.setText ( "" );
        textSharePath.setText ( "" );
        textNumberSubDirs.setText ( "" );
        textNumberFiles.setText ( "" );
        btnDefaultDownloadLocation.setSelection ( false );
        selectedShare = null;
        selectedShareFiles = null;
        ISelection isel = shareComboViewer.getSelection();
        ISelection isel2 = comboShareNameViewer.getSelection();

        if ( isel.isEmpty() )
        {
            if ( lst.size() > 0 )
            {
                selectedShare = lst.get ( 0 );
                StructuredSelection ss = new StructuredSelection ( selectedShare );
                shareComboViewer.setSelection ( ss );
            }

        }

        if ( isel2.isEmpty() )
        {
            if ( plst.size() > 0 )
            {
                selectedShareFiles = plst.get ( 0 );
                StructuredSelection ss = new StructuredSelection ( selectedShareFiles );
                comboShareNameViewer.setSelection ( ss );
            }

        }

        updateShareCount();
    }

    private void updateShareCount()
    {
        if ( selectedShare != null )
        {
            DirectoryShare ds = getNode().getShareManager().getShare ( selectedShare.getId() );
            textNumberFiles.setText ( Long.toString ( ds.getNumberFiles() ) );
            textNumberSubDirs.setText ( Long.toString ( ds.getNumberSubFolders() ) );
            textShareName.setText ( ds.getShareName() );
            textSharePath.setText ( ds.getDirectory() );
            btnDefaultDownloadLocation.setSelection ( ds.isDefaultDownload() );
        }

    }

    private String sortFileField1;
    private String sortFileField2;
    private boolean sortFileReverse;
    private SortField.Type sortFileType1;
    private SortField.Type sortFileType2;
    private Text bannerText;

    private void filesSearch ( String srch )
    {
        if ( selectedCommunity != null )
        {
            CObjList oldlst = ( CObjList ) fileTableViewer.getInput();
            Sort s = new Sort();

            if ( sortFileField1 != null )
            {
                if ( sortFileField2 == null )
                {
                    if ( SortedNumericSortField.Type.LONG.equals ( sortFileType1 ) )
                    {
                        s.setSort ( new SortedNumericSortField ( sortFileField1, sortFileType1, sortFileReverse ) );
                    }

                    else
                    {
                        s.setSort ( new SortField ( sortFileField1, sortFileType1, sortFileReverse ) );
                    }

                }

                else
                {
                    SortField sf1 = null;
                    SortField sf2 = null;

                    if ( SortedNumericSortField.Type.LONG.equals ( sortFileType1 ) )
                    {
                        sf1 = new SortedNumericSortField ( sortFileField1, sortFileType1, sortFileReverse );
                    }

                    else
                    {
                        sf1 = new SortField ( sortFileField1, sortFileType1, sortFileReverse );
                    }

                    if ( SortedNumericSortField.Type.LONG.equals ( sortFileType2 ) )
                    {
                        sf2 = new SortedNumericSortField ( sortFileField2, sortFileType2, sortFileReverse );
                    }

                    else
                    {
                        sf2 = new SortField ( sortFileField2, sortFileType2, sortFileReverse );
                    }

                    s.setSort ( sf1, sf2 );
                }

            }

            else
            {
                s.setSort ( new SortField ( CObj.docString ( CObj.NAME ), SortField.Type.STRING, false ) );
            }

            String fshare = null;

            if ( selectedShareFiles != null && selectedShareFiles.getId() != -1 )
            {
                fshare = selectedShareFiles.getShareName();
            }

            CObjList clst = getNode().getIndex().searchFiles ( selectedCommunity.getDig(), fshare, srch, s );
            fileTableViewer.setInput ( clst );

            if ( oldlst != null )
            {
                oldlst.close();
            }

        }

    }

    private SortField.Type membershipSortType = SortField.Type.STRING;
    private String membershipSortField = CObj.docPrivate ( CObj.PRV_DISPLAY_NAME );
    private boolean membershipSortReverse = false;

    private void updateMembership()
    {
        CObjList oldlst = ( CObjList ) membershipTableViewer.getInput();
        Sort s = new Sort();
        s.setSort ( new SortField ( membershipSortField, membershipSortType, membershipSortReverse ) );
        CObjList newlst = getNode().getIndex().getMyValidMemberships ( s );
        membershipTableViewer.setInput ( newlst );

        if ( oldlst != null )
        {
            oldlst.close();
        }

    }

    private String getPostString ( CObj pst )
    {
        StringBuilder msg = new StringBuilder();

        if ( pst != null )
        {
            String subj = pst.getString ( CObj.SUBJECT );
            String body = pst.getText ( CObj.BODY );
            String auth = pst.getString ( CObj.CREATOR_NAME );
            Long ts = pst.getNumber ( CObj.CREATEDON );

            msg.append ( "FROM: " );

            if ( auth != null )
            {
                msg.append ( auth );
            }

            msg.append ( "\n" );

            msg.append ( "ON: " );

            if ( ts != null )
            {
                msg.append ( ( new Date ( ts ) ).toString() );
            }

            msg.append ( "\n" );

            msg.append ( "SUBJ: " );

            if ( subj != null )
            {
                msg.append ( subj );
            }

            msg.append ( "\n" );

            msg.append ( "--------------------------------------------\n" );

            Set<String> fld = pst.listFields();

            for ( String fid : fld )
            {
                CObj f = getNode().getIndex().getByDig ( fid );

                if ( f != null )
                {
                    String nm = f.getString ( CObj.FLD_NAME );
                    String tp = f.getString ( CObj.FLD_TYPE );
                    String dsc = f.getString ( CObj.FLD_DESC );
                    String vs = null;

                    if ( CObj.FLD_TYPE_BOOL.equals ( tp ) )
                    {
                        Boolean bv = pst.getFieldBoolean ( fid );

                        if ( bv != null )
                        {
                            vs = bv.toString();
                        }

                    }

                    if ( CObj.FLD_TYPE_DECIMAL.equals ( tp ) )
                    {
                        Double db = pst.getFieldDecimal ( fid );

                        if ( db != null )
                        {
                            vs = db.toString();
                        }

                    }

                    if ( CObj.FLD_TYPE_NUMBER.equals ( tp ) )
                    {
                        Long lv = pst.getFieldNumber ( fid );

                        if ( lv != null )
                        {
                            vs = lv.toString();
                        }

                    }

                    if ( CObj.FLD_TYPE_STRING.equals ( tp ) )
                    {
                        vs = pst.getFieldString ( fid );
                    }

                    if ( vs != null )
                    {
                        vs.replace ( "\n", " " );
                        vs.replace ( "\r", "" );
                        dsc.replace ( "\n", " " );
                        dsc.replace ( "\r", "" );
                        String fldline = String.format ( "%15s:%-20s | %20s | %s",
                                                         nm, vs,
                                                         idCache.getName ( pst.getString ( CObj.CREATOR ) ),
                                                         dsc );
                        fldline = fldline.substring ( 0, Math.min ( fldline.length(), 79 ) );
                        msg.append ( fldline );
                        msg.append ( "\n" );
                    }

                }

            }

            msg.append ( "--------------------------------------------\n" );

            if ( body != null )
            {
                msg.append ( body );
            }

        }

        return msg.toString();
    }

    private void loadSeed ( File f )
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );

                if ( CObj.IDENTITY.equals ( co.getType() ) )
                {
                    co.setType ( CObj.USR_SEED );
                    node.enqueue ( co );
                }

                o = new JSONObject ( p );
            }

            br.close();
        }

        catch ( Exception e )
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    private void loadDeveloperIdentity ( File f )
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );

            if ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );
                developerIdentity = co;

            }

            br.close();
        }

        catch ( Exception e )
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    private void loadDefCommunitySubs ( File f )
    {
        BufferedReader br = null;
        List<CObj> comlst = new LinkedList<CObj>();

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );

                if ( CObj.COMMUNITY.equals ( co.getType() ) )
                {
                    co.setType ( CObj.USR_COMMUNITY );
                    comlst.add ( co );
                }

                o = new JSONObject ( p );
            }

            br.close();
        }

        catch ( Exception e )
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        if ( comlst.size() > 0 )
        {
            new DefComSubThread ( node, comlst );
        }

    }

    protected void createDialogs()
    {
        newCommunityDialog = new NewCommunityDialog ( shell, this );
        newCommunityDialog.create();
        hasFileDialog = new ShowHasFileDialog ( shell, this );
        hasFileDialog.create();
        newIdentityDialog = new NewIdentityDialog ( shell, this );
        newIdentityDialog.create();
        subscriptionDialog = new SubscriptionDialog ( shell, this );
        subscriptionDialog.create();
        newMemberDialog = new NewMemberDialog ( shell, this );
        newMemberDialog.create();
        newPostDialog = new NewPostDialog ( shell, this );
        newPostDialog.create();
        downloadPriorityDialog = new DownloadPriorityDialog ( shell, this );
        downloadPriorityDialog.create();
        downloadTableViewer.setInput ( getNode().getFileHandler() );
        membersDialog = new ShowMembersDialog ( shell, this );
        membersDialog.create();
        privComDialog = new ShowPrivComDialog ( shell, this );
        privComDialog.create();
        shareDialog = new NewDirectoryShareDialog ( shell, this );
        shareDialog.create();
        downloadToShareDialog = new DownloadToShareDialog ( shell, this );
        downloadToShareDialog.create();
        addFolderDialog = new AddFolderDialog ( shell, this );
        addFolderDialog.create();
        advancedDialog = new AdvancedSearchDialog ( shell, this );
        advancedDialog.create();
        localFileColumnProvider.setIndex ( node.getIndex() );
        updateMembership();
    }

    private void exportCommunities()
    {
        if ( exportCommunitiesFile != null )
        {
            try
            {
                File exf = new File ( exportCommunitiesFile );
                CObjList pubcoms = node.getIndex().getPublicCommunities();
                PrintWriter pw = new PrintWriter ( new FileOutputStream ( exf ) );

                for ( int c = 0; c < pubcoms.size(); c++ )
                {
                    CObj i = pubcoms.get ( c );
                    JSONObject jo = i.getJSON();
                    jo.write ( pw );
                    pw.println();
                }

                pw.close();
                pubcoms.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

    }

    public static int MAXIMGWIDTH = 400;
    private boolean resize = true;
    private int imagex = 0;
    private int imagey = 0;

    private void addImage ( Image image, int offset )
    {
        StyleRange style = new StyleRange ();
        style.start = offset;
        style.length = 1;
        style.data = image;
        Rectangle rect = image.getBounds();
        int w = rect.width;
        int h = rect.height;

        //resize = true;
        style.metrics = new GlyphMetrics ( h, 0, w );
        postText.setStyleRange ( style );
    }


    private Composite composite_6;
    private LocalFileColumnLabelProvider localFileColumnProvider;
    private Table membershipTable;
    private TableViewer membershipTableViewer;
    private CObjListContentProvider membershipProvider;
    private Composite composite_header;
    private Label lblError;
    private Text textShareName;
    private Text textSharePath;
    private Text textNumberSubDirs;
    private Text textNumberFiles;
    private Combo shareCombo;
    private ComboViewer shareComboViewer;
    private DirectoryShare selectedShare;
    private DirectoryShare selectedShareFiles;
    private Combo comboShareName;
    private ComboViewer comboShareNameViewer;
    private Button btnDefaultDownloadLocation;
    private Text txtAShareIs;

    private boolean doDownloadLrg ( CObj c )
    {
        String lrgfile = c.getString ( CObj.NAME );

        if ( lrgfile != null && selectedIdentity != null )
        {
            CObj p = new CObj();
            p.setType ( CObj.USR_DOWNLOAD_FILE );
            p.pushString ( CObj.CREATOR, selectedIdentity.getId() );
            p.pushString ( CObj.NAME, c.getString ( CObj.NAME ) );
            p.pushString ( CObj.COMMUNITYID, c.getString ( CObj.COMMUNITYID ) );
            p.pushNumber ( CObj.FILESIZE, c.getNumber ( CObj.FILESIZE ) );
            p.pushString ( CObj.FRAGDIGEST, c.getString ( CObj.FRAGDIGEST ) );
            p.pushNumber ( CObj.FRAGSIZE, c.getNumber ( CObj.FRAGSIZE ) );
            p.pushNumber ( CObj.FRAGNUMBER, c.getNumber ( CObj.FRAGNUMBER ) );
            p.pushString ( CObj.FILEDIGEST, c.getString ( CObj.FILEDIGEST ) );
            p.pushString ( CObj.SHARE_NAME, c.getString ( CObj.SHARE_NAME ) );
            getNode().enqueue ( p );
            return true;
        }

        return false;
    }

    private boolean doDownloadPrv ( CObj c )
    {
        String lrgfile = c.getString ( CObj.PRV_NAME );

        if ( lrgfile != null && selectedIdentity != null )
        {
            CObj p = new CObj();
            p.setType ( CObj.USR_DOWNLOAD_FILE );
            p.pushString ( CObj.CREATOR, selectedIdentity.getId() );
            p.pushString ( CObj.COMMUNITYID, c.getString ( CObj.COMMUNITYID ) );
            p.pushString ( CObj.NAME, c.getString ( CObj.PRV_NAME ) );
            p.pushNumber ( CObj.FILESIZE, c.getNumber ( CObj.PRV_FILESIZE ) );
            p.pushString ( CObj.FRAGDIGEST, c.getString ( CObj.PRV_FRAGDIGEST ) );
            p.pushNumber ( CObj.FRAGSIZE, c.getNumber ( CObj.PRV_FRAGSIZE ) );
            p.pushNumber ( CObj.FRAGNUMBER, c.getNumber ( CObj.PRV_FRAGNUMBER ) );
            p.pushString ( CObj.FILEDIGEST, c.getString ( CObj.PRV_FILEDIGEST ) );
            p.pushString ( CObj.SHARE_NAME, c.getString ( CObj.SHARE_NAME ) );
            getNode().enqueue ( p );
            return true;
        }

        return false;
    }

    protected void downloadLargeFile ( CObj c )
    {
        if ( !doDownloadLrg ( c ) )
        {
            doDownloadPrv ( c );
        }

    }

    protected void downloadPreview ( CObj c )
    {
        if ( !doDownloadPrv ( c ) )
        {
            doDownloadLrg ( c );
        }

    }

    private File getPreviewHasFile ( String comid, String wdig, String pdig, Long fsize )
    {
        File file = null;

        if ( comid != null && wdig != null && pdig != null &&
                fsize != null && fsize < 5L * 1024L * 1024L )
        {
            CObjList clst = node.getIndex().getMyHasFiles ( comid, wdig, pdig );

            if ( clst != null )
            {
                if ( clst.size() > 0 )
                {
                    try
                    {
                        CObj pc = clst.get ( 0 );
                        String lfs = pc.getPrivate ( CObj.LOCALFILE );

                        if ( lfs != null )
                        {
                            File tf = new File ( lfs );

                            if ( tf.exists() )
                            {
                                file = tf;
                            }

                        }

                    }

                    catch ( Exception e2 )
                    {
                        e2.printStackTrace();
                    }

                }

                clst.close();
            }

        }

        File rf = null;

        if ( file != null )
        {
            String fname = file.getPath();
            String imgtypes[] = new String[] {".jpg",
                                              ".jpeg", ".gif", ".png", ".bmp", ".tiff",
                                              ".JPG",
                                              ".JPEG", ".GIF", ".PNG", ".BMP", ".TIFF"
                                             };

            for ( int c = 0; c < imgtypes.length && rf == null; c++ )
            {
                if ( fname.endsWith ( imgtypes[c] ) )
                {
                    rf = file;
                }

            }

        }

        return rf;
    }

    public static ImageRegistry imgReg;

    public void setAdvancedQuery ( CObj q )
    {
        advQuery = q;
        searchText.setText ( "" );
        postSearch();
    }

    /**
        Create contents of the window.
    */
    protected void createContents()
    {
        if ( SWT.getPlatform().equals ( "cocoa" ) )
        {
            new CocoaUIEnhancer().earlyStartup();
        }

        imgReg = new ImageRegistry();

        shell = new Shell();
        shell.setSize ( 750, 550 );

        try
        {
            Image icons[] = new Image[5];
            ClassLoader loader = SWTApp.class.getClassLoader();

            InputStream is16  = loader.getResourceAsStream ( "images/aktie16.png" );
            icons[0] = new Image ( Display.getDefault(), is16 );
            is16.close();

            InputStream is32  = loader.getResourceAsStream ( "images/aktie32.png" );
            icons[1] = new Image ( Display.getDefault(), is32 );
            is32.close();

            InputStream is64  = loader.getResourceAsStream ( "images/aktie64.png" );
            icons[2] = new Image ( Display.getDefault(), is64 );
            is64.close();

            InputStream is128  = loader.getResourceAsStream ( "images/aktie128.png" );
            icons[3] = new Image ( Display.getDefault(), is128 );
            is128.close();

            InputStream is  = loader.getResourceAsStream ( "images/aktie.png" );
            icons[4] = new Image ( Display.getDefault(), is );
            is.close();

            shell.setImages ( icons );

            InputStream pi  = loader.getResourceAsStream ( "images/icons/user.png" );
            Image pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "identity", pimg );

            pi  = loader.getResourceAsStream ( "images/icons/database.png" );
            pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "pubsub", pimg );

            pi  = loader.getResourceAsStream ( "images/icons/key.png" );
            pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "privsub", pimg );

            pi  = loader.getResourceAsStream ( "images/icons/folder.png" );
            pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "folder", pimg );

            pi  = loader.getResourceAsStream ( "images/icons/bullet-red.png" );
            pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "offline", pimg );

            pi  = loader.getResourceAsStream ( "images/icons/bullet-green.png" );
            pimg = new Image ( Display.getDefault(), pi );
            pi.close();
            imgReg.put ( "online", pimg );

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        shell.setText ( "aktie" );
        shell.setLayout ( new GridLayout ( 1, false ) );

        Menu menu = new Menu ( shell, SWT.BAR );
        shell.setMenuBar ( menu );

        MenuItem mntmFile = new MenuItem ( menu, SWT.CASCADE );
        mntmFile.setText ( "File" );

        Menu menu_1 = new Menu ( mntmFile );
        mntmFile.setMenu ( menu_1 );

        doUpgrade = Wrapper.getAutoUpdate();

        final MenuItem mntmAutoupdate = new MenuItem ( menu_1, SWT.NONE );
        mntmAutoupdate.setText ( doUpgrade ? "Disable auto upgrade" : "Enable auto upgrade" );
        mntmAutoupdate.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                doUpgrade = !doUpgrade;
                Wrapper.saveAutoUpdate ( doUpgrade );
                mntmAutoupdate.setText ( doUpgrade ? "Disable auto upgrade" : "Enable auto upgrade" );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmLoadSeedFile = new MenuItem ( menu_1, SWT.NONE );
        mntmLoadSeedFile.setText ( "Load Seed File" );
        mntmLoadSeedFile.addSelectionListener ( new LoadSeeds() );

        MenuItem mntmSaveSeedFile = new MenuItem ( menu_1, SWT.NONE );
        mntmSaveSeedFile.setText ( "Save Seed File" );
        mntmSaveSeedFile.addSelectionListener ( new SaveSeeds() );

        MenuItem mntmShowlocked = new MenuItem ( menu_1, SWT.NONE );
        mntmShowlocked.setText ( "Show Locked Communities" );
        mntmShowlocked.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                privComDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmStartManualUpdate = new MenuItem ( menu_1, SWT.NONE );
        mntmStartManualUpdate.setText ( "Refresh All Now" );
        mntmStartManualUpdate.addSelectionListener ( new ManualUpdate() );

        MenuItem mntmI2Popts = new MenuItem ( menu_1, SWT.NONE );
        mntmI2Popts.setText ( "I2P Options" );
        mntmI2Popts.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                i2pDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        composite_header = new Composite ( shell, SWT.NONE );
        composite_header.setLayout ( new GridLayout ( 3, false ) );
        composite_header.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );

        lblVersion = new Label ( composite_header, SWT.NONE );
        lblVersion.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        setVersionNetStatus();

        lblError = new Label ( composite_header, SWT.NONE );
        lblError.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblError.setText ( "" );
        new Label ( composite_header, SWT.NONE );

        TabFolder tabFolder = new TabFolder ( shell, SWT.NONE );
        tabFolder.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        TabItem tbtmCommunity = new TabItem ( tabFolder, SWT.NONE );
        tbtmCommunity.setText ( "Communities" );

        Composite composite = new Composite ( tabFolder, SWT.NONE );
        tbtmCommunity.setControl ( composite );
        composite.setLayout ( new FillLayout ( SWT.HORIZONTAL ) );

        SashForm sashForm = new SashForm ( composite, SWT.NONE );

        SashForm sashForm2 = new SashForm ( sashForm, SWT.VERTICAL );

        Composite composite_1 = new Composite ( sashForm2, SWT.NONE );
        composite_1.setLayout ( new FillLayout ( SWT.VERTICAL ) );

        identTreeViewer = new TreeViewer ( composite_1, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        identTree = identTreeViewer.getTree();

        identTreeViewer.addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @SuppressWarnings ( "rawtypes" )
            @Override
            public void selectionChanged ( SelectionChangedEvent s )
            {
                IStructuredSelection sel = ( IStructuredSelection ) s.getSelection();

                CObj id = null;
                CObj com = null;

                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity sm = ( SubTreeEntity ) selo;
                        CObj co = identModel.getCObj ( sm.getId() );

                        if ( co != null )
                        {

                            if ( CObj.IDENTITY.equals ( co.getType() ) )
                            {
                                id = co;
                            }

                            if ( CObj.COMMUNITY.equals ( co.getType() ) )
                            {
                                com = co;
                                id = identModel.getCObj ( sm.getIdentity() );
                            }

                        }

                    }

                }

                if ( id != null )
                {
                    selectedIdentity = id;

                    if ( com != null )
                    {
                        setSelected ( id, com );
                    }

                }


            }

        } );

        Menu menu_2 = new Menu ( identTree );
        identTree.setMenu ( menu_2 );

        MenuItem mntmSubscribe_1 = new MenuItem ( menu_2, SWT.NONE );
        mntmSubscribe_1.setText ( "Subscribe" );
        mntmSubscribe_1.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    subscriptionDialog.open ( selectedIdentity.getId() );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmUnsubscribe = new MenuItem ( menu_2, SWT.NONE );
        mntmUnsubscribe.setText ( "Unsubscribe" );
        mntmUnsubscribe.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                String selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity ts = ( SubTreeEntity ) selo;

                        if ( SubTreeEntity.PRVCOMMUNITY_TYPE == ts.getType() ||
                                SubTreeEntity.PUBCOMMUNITY_TYPE == ts.getType() )
                        {
                            String identid = ts.getIdentity();
                            CObj com = identModel.getCObj ( ts.getRefId() );

                            if ( com != null )
                            {
                                String comid = com.getDig();
                                CObj unsub = new CObj();
                                unsub.setType ( CObj.SUBSCRIPTION );
                                unsub.pushString ( CObj.COMMUNITYID, comid );
                                unsub.pushString ( CObj.CREATOR, identid );
                                unsub.pushString ( CObj.SUBSCRIBED, "flase" );
                                getNode().enqueue ( unsub );
                            }

                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmNewflder = new MenuItem ( menu_2, SWT.NONE );
        mntmNewflder.setText ( "New Folder" );
        mntmNewflder.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                String selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity et = ( SubTreeEntity ) selo;
                        addFolderDialog.open ( et );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmRmFolder = new MenuItem ( menu_2, SWT.NONE );
        mntmRmFolder.setText ( "Remove Folder" );
        mntmRmFolder.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                String selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity et = ( SubTreeEntity ) selo;
                        identModel.removeFolder ( et );
                        identTreeViewer.setInput ( "Folder removed" );
                        identModel.setCollaspseState ( identTreeViewer );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmShowmem = new MenuItem ( menu_2, SWT.NONE );
        mntmShowmem.setText ( "Show Members" );
        mntmShowmem.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedCommunity != null )
                {
                    membersDialog.open ( selectedCommunity );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmNewCommunity_1 = new MenuItem ( menu_2, SWT.NONE );
        mntmNewCommunity_1.setText ( "New Community" );
        mntmNewCommunity_1.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                String selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity ti = ( SubTreeEntity ) selo;
                        selid = ti.getIdentity();
                    }

                }

                newCommunityDialog.open ( selid );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmNewIdentity_1 = new MenuItem ( menu_2, SWT.NONE );
        mntmNewIdentity_1.setText ( "New Identity" );
        mntmNewIdentity_1.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                newIdentityDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmNewMember = new MenuItem ( menu_2, SWT.NONE );
        mntmNewMember.setText ( "New Member" );
        mntmNewMember.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    newMemberDialog.open ( selectedIdentity.getId(), selectedCommunity.getDig() );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmRefCom = new MenuItem ( menu_2, SWT.NONE );
        mntmRefCom.setText ( "Refresh Community" );
        mntmRefCom.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    updateCommunity ( selectedCommunity );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmClose = new MenuItem ( menu_2, SWT.NONE );
        mntmClose.setText ( "Disconnect Identity" );
        mntmClose.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                CObj selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity ti = ( SubTreeEntity ) selo;
                        selid = identModel.getCObj ( ti.getIdentity() );
                    }

                }

                if ( selid != null )
                {
                    CObj cl = selid.clone();
                    cl.setType ( CObj.USR_START_DEST );
                    cl.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 0L );

                    if ( node != null )
                    {
                        node.priorityEnqueue ( cl );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmConnect = new MenuItem ( menu_2, SWT.NONE );
        mntmConnect.setText ( "Connect Identity" );
        mntmConnect.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) identTreeViewer.getSelection();
                CObj selid = null;
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() && selid == null )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity ti = ( SubTreeEntity ) selo;
                        selid = identModel.getCObj ( ti.getIdentity() );
                    }


                }

                if ( selid != null )
                {
                    CObj cl = selid.clone();
                    cl.setType ( CObj.USR_START_DEST );
                    cl.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 1L );

                    if ( node != null )
                    {
                        node.priorityEnqueue ( cl );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );


        membershipTableViewer = new TableViewer ( sashForm2, SWT.BORDER | SWT.FULL_SELECTION );
        membershipTable = membershipTableViewer.getTable();
        membershipTable.setHeaderVisible ( true );
        membershipProvider = new CObjListContentProvider();
        membershipTableViewer.setContentProvider ( membershipProvider );

        TableViewerColumn mcol0 = new TableViewerColumn ( membershipTableViewer, SWT.NONE );
        mcol0.getColumn().setText ( "Memberships" );
        mcol0.getColumn().setWidth ( 170 );
        mcol0.setLabelProvider ( new CObjListPrivDispNameColumnLabelProvider ( ) );
        mcol0.getColumn().addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                membershipSortReverse = !membershipSortReverse;
                updateMembership();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Menu menu_6 = new Menu ( membershipTable );
        membershipTable.setMenu ( menu_6 );

        MenuItem mntmSubscribe = new MenuItem ( menu_6, SWT.NONE );
        mntmSubscribe.setText ( "Subscribe" );
        mntmSubscribe.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) membershipTableViewer.getSelection();
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                        CObj fr = ae.getCObj();

                        if ( fr != null )
                        {
                            String memid = null;
                            String comid = fr.getDig();
                            String creator = fr.getString ( CObj.CREATOR );
                            CObjList idlst = getNode().getIndex().getMyIdentities();

                            for ( int c = 0; c < idlst.size() && memid == null; c++ )
                            {
                                try
                                {
                                    String id = idlst.get ( c ).getId();

                                    if ( creator.equals ( id ) )
                                    {
                                        memid = id;
                                    }

                                }

                                catch ( Exception e2 )
                                {
                                    e2.printStackTrace();
                                }

                            }

                            idlst.close();

                            if ( memid == null )
                            {
                                CObjList mlst = getNode().getIndex().getMyMemberships ( comid );

                                if ( mlst.size() > 0 )
                                {
                                    try
                                    {
                                        CObj mm = mlst.get ( 0 );
                                        memid = mm.getPrivate ( CObj.MEMBERID );
                                    }

                                    catch ( Exception e2 )
                                    {
                                        e2.printStackTrace();
                                    }

                                }

                            }

                            if ( comid != null && memid != null )
                            {
                                //Create a subscription
                                CObj sub = new CObj();
                                sub.setType ( CObj.SUBSCRIPTION );
                                sub.pushString ( CObj.CREATOR, memid );
                                sub.pushString ( CObj.COMMUNITYID, comid );
                                sub.pushString ( CObj.SUBSCRIBED, "true" );
                                getNode().enqueue ( sub );

                            }

                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmShowPriv = new MenuItem ( menu_6, SWT.NONE );
        mntmShowPriv.setText ( "Show Locked Communities" );
        mntmShowPriv.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                privComDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {

            }

        } );

        sashForm2.setWeights ( new int[] {1, 1} );


        //scrolledComposite.setContent ( identTree );
        //scrolledComposite.setMinSize ( identTree.computeSize ( SWT.DEFAULT, SWT.DEFAULT ) );

        Composite composite_2 = new Composite ( sashForm, SWT.NONE );
        GridLayout gl_composite_2 = new GridLayout ( 1, false );
        composite_2.setLayout ( gl_composite_2 );

        lblIdentCommunity = new Label ( composite_2, SWT.NONE );
        lblIdentCommunity.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblIdentCommunity.setText ( "Identity: <id>  Community: <com>" );

        TabFolder tabFolder_1 = new TabFolder ( composite_2, SWT.NONE );
        GridData gd_tabFolder_1 = new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 );
        gd_tabFolder_1.heightHint = 235;
        tabFolder_1.setLayoutData ( gd_tabFolder_1 );

        TabItem tbtmPosts = new TabItem ( tabFolder_1, SWT.NONE );
        tbtmPosts.setText ( "Posts" );

        Composite composite_3 = new Composite ( tabFolder_1, SWT.NONE );
        tbtmPosts.setControl ( composite_3 );
        composite_3.setLayout ( new FillLayout ( SWT.HORIZONTAL ) );

        SashForm sashForm_1 = new SashForm ( composite_3, SWT.VERTICAL );

        Composite composite_5 = new Composite ( sashForm_1, SWT.NONE );
        composite_5.setLayout ( new BorderLayout ( 0, 0 ) );

        Composite composite_7 = new Composite ( composite_5, SWT.NONE );
        composite_7.setLayoutData ( BorderLayout.NORTH );
        composite_7.setLayout ( new GridLayout ( 7, false ) );
        new Label ( composite_7, SWT.NONE );

        Button btnPost = new Button ( composite_7, SWT.NONE );
        btnPost.setText ( "Post" );
        btnPost.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    newPostDialog.open ( selectedIdentity, selectedCommunity, null, null );
                }

                else
                {
                    MessageDialog.openWarning ( shell, "Select a community.", "Sorry, you have to select the community you wish to post to." );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Label label = new Label ( composite_7, SWT.SEPARATOR | SWT.VERTICAL );
        GridData gd_label = new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 );
        gd_label.heightHint = 25;
        label.setLayoutData ( gd_label );

        searchText = new Text ( composite_7, SWT.BORDER );
        searchText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        searchText.addKeyListener ( null );
        searchText.addListener ( SWT.Traverse, new Listener()
        {
            @Override
            public void handleEvent ( Event event )
            {
                if ( event.detail == SWT.TRAVERSE_RETURN )
                {
                    advQuery = null;
                    postSearch();
                }

            }

        } );

        Button btnSearch = new Button ( composite_7, SWT.NONE );
        btnSearch.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        btnSearch.setText ( "Search" );
        btnSearch.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String st = searchText.getText();
                Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( st );

                if ( m.find() )
                {
                    advQuery = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnAdvanced = new Button ( composite_7, SWT.NONE );
        btnAdvanced.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        btnAdvanced.setText ( "Advanced" );
        btnAdvanced.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                advancedDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnRefresh = new Button ( composite_7, SWT.NONE );
        btnRefresh.setText ( "Refresh" );
        btnRefresh.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    updateCommunity ( selectedCommunity );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        postTableViewer = new TableViewer ( composite_5, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        postTable = postTableViewer.getTable();
        postTable.setLayoutData ( BorderLayout.CENTER );
        postTable.setHeaderVisible ( true );
        postTable.setLinesVisible ( true );
        postContentProvider = new CObjListContentProvider();
        postTableViewer.setContentProvider ( postContentProvider );

        TableViewerColumn col0 = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col0.getColumn().setText ( "Identity" );
        col0.getColumn().setWidth ( 100 );
        col0.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.CREATOR_NAME ) );
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.CREATOR_NAME );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col1 = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col1.getColumn().setText ( "Subject" );
        col1.getColumn().setWidth ( 200 );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.SUBJECT ) );
        col1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.SUBJECT );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col2 = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col2.getColumn().setText ( "Date" );
        col2.getColumn().setWidth ( 100 );
        col2.setLabelProvider ( new CObjListDateColumnLabelProvider ( CObj.CREATEDON ) );
        col2.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docNumber ( CObj.CREATEDON );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = true;
                    sortPostType1 = SortedNumericSortField.Type.LONG;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col3 = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col3.getColumn().setText ( "File" );
        col3.getColumn().setWidth ( 100 );
        col3.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.NAME ) );
        col3.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.NAME );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col3b = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col3b.getColumn().setText ( "Preview" );
        col3b.getColumn().setWidth ( 100 );
        col3b.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.PRV_NAME ) );
        col3b.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.PRV_NAME );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        localFileColumnProvider = new LocalFileColumnLabelProvider();

        TableViewerColumn col4 = new TableViewerColumn ( postTableViewer, SWT.NONE );
        col4.getColumn().setText ( "Local File" );
        col4.getColumn().setWidth ( 100 );
        col4.setLabelProvider ( localFileColumnProvider );
        col4.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivate ( CObj.LOCALFILE );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                    sortPostField2 = null;
                    sortPostType2 = null;
                }

                postSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        postTableViewer.addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged ( SelectionChangedEvent e )
            {
                ISelection selection = e.getSelection();

                if ( selection != null && selection instanceof IStructuredSelection )
                {
                    IStructuredSelection ssel = ( IStructuredSelection ) selection;

                    if ( ssel.size() > 0 )
                    {
                        Object selo = ssel.getFirstElement();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement selm = ( CObjListArrayElement ) selo;
                            displayedPost = selm.getCObj();
                            displayedPost.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 0L );

                            try
                            {
                                node.getIndex().index ( displayedPost );
                            }

                            catch ( IOException e1 )
                            {
                                e1.printStackTrace();
                            }

                            postSearch();

                            String msgdisp = getPostString ( displayedPost );
                            msgdisp = NewPostDialog.formatDisplay ( msgdisp, false );
                            String lines = "\n==========================\n=";
                            String msg = msgdisp + lines;
                            postText.setText ( msg );

                            //String comid, String wdig, String pdig
                            String comid = displayedPost.getString ( CObj.COMMUNITYID );

                            String pwdig = displayedPost.getString ( CObj.PRV_FILEDIGEST );
                            String ppdig = displayedPost.getString ( CObj.PRV_FRAGDIGEST );
                            Long pfsize = displayedPost.getNumber ( CObj.PRV_FILESIZE );

                            File prvfile = getPreviewHasFile ( comid, pwdig, ppdig, pfsize );

                            String wdig = displayedPost.getString ( CObj.FILEDIGEST );
                            String pdig = displayedPost.getString ( CObj.FRAGDIGEST );
                            Long fsize = displayedPost.getNumber ( CObj.FILESIZE );

                            File file = getPreviewHasFile ( comid, wdig, pdig, fsize );

                            if ( prvfile != null )
                            {
                                Display defdesp = Display.getDefault();
                                Image image = new Image ( defdesp, prvfile.getPath() );
                                addImage ( image, msg.length() - 1 );
                            }

                            else if ( file != null )
                            {
                                Display defdesp = Display.getDefault();

                                try
                                {
                                    Image image = new Image ( defdesp, file.getPath() );
                                    addImage ( image, msg.length() - 1 );
                                }

                                catch ( Exception ei )
                                {
                                    //do not spam the user.  this could happen a lot if someone
                                    //is a jerk
                                }

                            }

                        }

                    }

                }

            }

        } );

        Menu menu_5 = new Menu ( postTable );
        postTable.setMenu ( menu_5 );

        MenuItem mntmDownloadFile2 = new MenuItem ( menu_5, SWT.NONE );
        mntmDownloadFile2.setText ( "Download File(s)" );
        mntmDownloadFile2.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();
                            downloadLargeFile ( fr );
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmDownloadPrv = new MenuItem ( menu_5, SWT.NONE );
        mntmDownloadPrv.setText ( "Download Preview(s)" );
        mntmDownloadPrv.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();
                            downloadPreview ( fr );
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmDownloadFile22 = new MenuItem ( menu_5, SWT.NONE );
        mntmDownloadFile22.setText ( "Download File(s) to Share.." );
        mntmDownloadFile22.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    downloadToShareDialog.open ( sel, false );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmDownloadPrv2 = new MenuItem ( menu_5, SWT.NONE );
        mntmDownloadPrv2.setText ( "Download Preview(s) to Share.." );
        mntmDownloadPrv2.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    downloadToShareDialog.open ( sel, true );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmShowHas = new MenuItem ( menu_5, SWT.NONE );
        mntmShowHas.setText ( "Who Has File" );
        mntmShowHas.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();
                            hasFileDialog.open ( fr );
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmReply = new MenuItem ( menu_5, SWT.NONE );
        mntmReply.setText ( "Reply" );
        mntmReply.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) postTableViewer.getSelection();

                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj pst = ae.getCObj();
                            newPostDialog.reply ( selectedIdentity, selectedCommunity, pst );

                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        composite_6 = new Composite ( sashForm_1, SWT.NONE );
        composite_6.setLayout ( new GridLayout() );

        postText = new StyledText ( composite_6, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI );
        postText.setFont ( JFaceResources.getFont ( JFaceResources.TEXT_FONT ) );

        postText.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true ) );
        postText.setEditable ( false );
        postText.setCaret ( null );

        // use a verify listener to dispose the images
        postText.addVerifyListener ( new VerifyListener()
        {
            public void verifyText ( VerifyEvent event )
            {
                if ( event.start == event.end ) { return; }

                String text = postText.getText(); //getText(event.start, event.end - 1);
                int index = text.length() - 1;
                StyleRange style = postText.getStyleRangeAtOffset ( index );

                if ( style != null )
                {
                    Image image = ( Image ) style.data;

                    if ( image != null ) { image.dispose(); }

                }

            }

        } );

        // draw images on paint event
        postText.addPaintObjectListener ( new PaintObjectListener()
        {
            @Override
            public void paintObject ( PaintObjectEvent event )
            {
                StyleRange style = event.style;
                Image image = ( Image ) style.data;

                if ( !image.isDisposed() )
                {
                    imagex = event.x;
                    imagey = event.y + event.ascent - style.metrics.ascent;
                    int w = image.getBounds().width;
                    int h = image.getBounds().height;
                    int sw = w;
                    int sh = h;

                    if ( resize )
                    {
                        if ( sw > MAXIMGWIDTH )
                        {
                            sh = sh * MAXIMGWIDTH / sw;
                            sw = MAXIMGWIDTH;
                        }

                    }

                    event.gc.setAntialias ( SWT.ON );
                    event.gc.drawImage ( image, 0, 0,
                                         image.getBounds().width, image.getBounds().height,
                                         imagex, imagey, sw, sh );
                }

            }

        } );

        postText.addListener ( SWT.Dispose, new Listener()
        {
            public void handleEvent ( Event event )
            {
                StyleRange[] styles = postText.getStyleRanges();

                for ( int i = 0; i < styles.length; i++ )
                {
                    StyleRange style = styles[i];

                    if ( style.data != null )
                    {
                        Image image = ( Image ) style.data;

                        if ( image != null ) { image.dispose(); }

                    }

                }

            }

        } );

        postText.addMouseListener ( new MouseListener()
        {
            @Override
            public void mouseDoubleClick ( MouseEvent e )
            {
                resize = !resize;
                postText.redraw();
                postText.setSelection ( 0, 0 );
            }

            @Override
            public void mouseDown ( MouseEvent e )
            {
            }

            @Override
            public void mouseUp ( MouseEvent e )
            {
            }

        } );


        sashForm_1.setWeights ( new int[] {1, 1} );

        TabItem tbtmFiles = new TabItem ( tabFolder_1, SWT.NONE );
        tbtmFiles.setText ( "Files" );

        Composite composite_4 = new Composite ( tabFolder_1, SWT.NONE );
        tbtmFiles.setControl ( composite_4 );
        composite_4.setLayout ( new BorderLayout ( 0, 0 ) );

        Composite composite_9 = new Composite ( composite_4, SWT.NONE );
        composite_9.setLayoutData ( BorderLayout.NORTH );
        composite_9.setLayout ( new GridLayout ( 6, false ) );

        Button btnAddFiles = new Button ( composite_9, SWT.NONE );
        btnAddFiles.setText ( "Add File(s)" );
        btnAddFiles.addSelectionListener ( new AddFile() );

        Label label_1 = new Label ( composite_9, SWT.SEPARATOR | SWT.VERTICAL );
        GridData gd_label_1 = new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 );
        gd_label_1.heightHint = 25;
        label_1.setLayoutData ( gd_label_1 );

        comboShareNameViewer = new ComboViewer ( composite_9, SWT.NONE | SWT.READ_ONLY );
        comboShareNameViewer.setContentProvider ( new DirectoryShareContentProvider() );
        comboShareNameViewer.setLabelProvider ( new DirectoryShareLabelProvider() );
        comboShareName = comboShareNameViewer.getCombo();
        comboShareName.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        comboShareName.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) comboShareNameViewer.getSelection();
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    DirectoryShare sh = ( DirectoryShare ) i.next();
                    selectedShareFiles = sh;
                    filesSearch();
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        fileSearch = new Text ( composite_9, SWT.BORDER );
        fileSearch.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Button btnSearch_1 = new Button ( composite_9, SWT.NONE );
        btnSearch_1.setText ( "Search" );
        btnSearch_1.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedCommunity != null )
                {
                    filesSearch();
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );


        Button btnRefresh_1 = new Button ( composite_9, SWT.NONE );
        btnRefresh_1.setText ( "Refresh" );
        btnRefresh_1.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    updateCommunity ( selectedCommunity );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        fileContentProvider = new CObjListContentProvider();
        fileTableViewer = new TableViewer ( composite_4, SWT.BORDER | SWT.FULL_SELECTION |
                                            SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI );
        fileTableViewer.setContentProvider ( fileContentProvider );
        fileTable = fileTableViewer.getTable();
        fileTable.setHeaderVisible ( true );
        fileTable.setLinesVisible ( true );
        fileTable.setLayoutData ( BorderLayout.CENTER );
        sashForm.setWeights ( new int[] {1, 4} );

        TableViewerColumn fcol0 = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol0.getColumn().setText ( "File" );
        fcol0.getColumn().setWidth ( 100 );
        fcol0.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.NAME ) );
        fcol0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.NAME );

                if ( ns.equals ( sortFileField1 ) )
                {
                    sortFileReverse = !sortFileReverse;
                }

                else
                {
                    sortFileField1 = ns;
                    sortFileReverse = false;
                    sortFileType1 = SortField.Type.STRING;
                    sortFileField2 = null;
                    sortFileType2 = null;
                }

                filesSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn fcol1 = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol1.getColumn().setText ( "Size" );
        fcol1.getColumn().setWidth ( 100 );
        fcol1.setLabelProvider ( new CObjListLongColumnLabelProvider ( CObj.FILESIZE ) );
        fcol1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docNumber ( CObj.FILESIZE );

                if ( ns.equals ( sortFileField1 ) )
                {
                    sortFileReverse = !sortFileReverse;
                }

                else
                {
                    sortFileField1 = ns;
                    sortFileReverse = false;
                    sortFileType1 = SortedNumericSortField.Type.LONG;
                    sortFileField2 = null;
                    sortFileType2 = null;
                }

                filesSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn fcol2 = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol2.getColumn().setText ( "Sha256" );
        fcol2.getColumn().setWidth ( 100 );
        fcol2.setLabelProvider ( new CObjListHexColumnLabelProvider ( CObj.FILEDIGEST ) );
        fcol2.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.FILEDIGEST );

                if ( ns.equals ( sortFileField1 ) )
                {
                    sortFileReverse = !sortFileReverse;
                }

                else
                {
                    sortFileField1 = ns;
                    sortFileReverse = false;
                    sortFileType1 = SortField.Type.STRING;
                    sortFileField2 = null;
                    sortFileType2 = null;
                }

                filesSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn fcol2a = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol2a.getColumn().setText ( "Status" );
        fcol2a.getColumn().setWidth ( 70 );
        fcol2a.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.STATUS ) );
        fcol2a.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.STATUS );

                if ( ns.equals ( sortFileField1 ) )
                {
                    sortFileReverse = !sortFileReverse;
                }

                else
                {
                    sortFileField1 = ns;
                    sortFileReverse = false;
                    sortFileType1 = SortField.Type.STRING;
                    sortFileField2 = null;
                    sortFileType2 = null;
                }

                filesSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn fcol3 = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol3.getColumn().setText ( "Local File" );
        fcol3.getColumn().setWidth ( 100 );
        fcol3.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.LOCALFILE ) );
        fcol3.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.LOCALFILE );

                if ( ns.equals ( sortFileField1 ) )
                {
                    sortFileReverse = !sortFileReverse;
                }

                else
                {
                    sortFileField1 = ns;
                    sortFileReverse = false;
                    sortFileType1 = SortField.Type.STRING;
                    sortFileField2 = null;
                    sortFileType2 = null;
                }

                filesSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn fcol4 = new TableViewerColumn ( fileTableViewer, SWT.NONE );
        fcol4.getColumn().setText ( "Number Has" );
        fcol4.getColumn().setWidth ( 50 );
        fcol4.setLabelProvider ( new CObjListLongColumnLabelProvider ( CObj.NUMBER_HAS ) );

        Menu menu_3 = new Menu ( fileTable );
        fileTable.setMenu ( menu_3 );

        MenuItem mntmDownloadFile = new MenuItem ( menu_3, SWT.NONE );
        mntmDownloadFile.setText ( "Download File" );
        mntmDownloadFile.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) fileTableViewer.getSelection();

                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();
                            fr.setType ( CObj.USR_DOWNLOAD_FILE );
                            fr.pushString ( CObj.CREATOR, selectedIdentity.getId() );
                            getNode().enqueue ( fr );
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmDownloadFile3 = new MenuItem ( menu_3, SWT.NONE );
        mntmDownloadFile3.setText ( "Download File to share.." );
        mntmDownloadFile3.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) fileTableViewer.getSelection();

                    downloadToShareDialog.open ( sel, true );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmCreatePost = new MenuItem ( menu_3, SWT.NONE );
        mntmCreatePost.setText ( "Attach to Post" );
        mntmCreatePost.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    CObj f1 = null;
                    CObj f2 = null;
                    IStructuredSelection sel = ( IStructuredSelection ) fileTableViewer.getSelection();
                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    while ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();

                            if ( f2 == null )
                            {
                                f2 = fr;
                            }

                            else
                            {
                                long f2size = f2.getNumber ( CObj.FILESIZE );
                                long frsize = fr.getNumber ( CObj.FILESIZE );

                                if ( f2size < frsize )
                                {
                                    f1 = f2;
                                    f2 = fr;
                                }

                                else
                                {
                                    f1 = fr;
                                }

                            }

                        }

                    }

                    if ( f2 != null )
                    {
                        newPostDialog.open ( selectedIdentity, selectedCommunity, f1, f2 );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem mntmShowhas = new MenuItem ( menu_3, SWT.NONE );
        mntmShowhas.setText ( "Who Has File" );
        mntmShowhas.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedCommunity != null )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) fileTableViewer.getSelection();
                    @SuppressWarnings ( "rawtypes" )
                    Iterator i = sel.iterator();

                    if ( i.hasNext() )
                    {
                        Object selo = i.next();

                        if ( selo instanceof CObjListArrayElement )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                            CObj fr = ae.getCObj();
                            hasFileDialog.open ( fr );
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );


        //============================================================================================
        TabItem tbtmShare = new TabItem ( tabFolder_1, SWT.NONE );
        tbtmShare.setText ( "Shares" );

        Composite composite_14 = new Composite ( tabFolder_1, SWT.NONE );
        tbtmShare.setControl ( composite_14 );
        composite_14.setLayout ( new GridLayout ( 2, false ) );

        Button btnShare = new Button ( composite_14, SWT.NONE );
        btnShare.setText ( "Add Share Directory" );
        btnShare.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedIdentity != null && selectedCommunity != null )
                {
                    shareDialog.open ( selectedIdentity, selectedCommunity );
                    setShares ( selectedCommunity.getDig(), selectedIdentity.getId() );

                }

                else
                {
                    MessageDialog.openWarning ( shell, "Select a community.", "Sorry, you have to select the community you wish to share with." );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        shareComboViewer = new ComboViewer ( composite_14, SWT.NONE | SWT.READ_ONLY );
        shareComboViewer.setContentProvider ( new DirectoryShareContentProvider() );
        shareComboViewer.setLabelProvider ( new DirectoryShareLabelProvider() );
        shareCombo = shareComboViewer.getCombo();
        shareCombo.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        shareCombo.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) shareComboViewer.getSelection();
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    DirectoryShare sh = ( DirectoryShare ) i.next();
                    selectedShare = sh;
                    updateShareCount();
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Label lblName = new Label ( composite_14, SWT.NONE );
        lblName.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblName.setText ( "Name" );

        textShareName = new Text ( composite_14, SWT.BORDER );
        textShareName.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        textShareName.setEditable ( false );

        Label lblPath = new Label ( composite_14, SWT.NONE );
        lblPath.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblPath.setText ( "Path" );

        textSharePath = new Text ( composite_14, SWT.BORDER );
        textSharePath.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        textSharePath.setEditable ( false );

        Label lblNumberOfSubdirectories = new Label ( composite_14, SWT.NONE );
        lblNumberOfSubdirectories.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblNumberOfSubdirectories.setText ( "Number of Subdirectories" );

        textNumberSubDirs = new Text ( composite_14, SWT.BORDER );
        textNumberSubDirs.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        textNumberSubDirs.setEditable ( false );

        Label lblNumberOfFiles = new Label ( composite_14, SWT.NONE );
        lblNumberOfFiles.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblNumberOfFiles.setText ( "Number of files" );

        textNumberFiles = new Text ( composite_14, SWT.BORDER );
        textNumberFiles.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        textNumberFiles.setEditable ( false );
        new Label ( composite_14, SWT.NONE );

        new Label ( composite_14, SWT.NONE );

        btnDefaultDownloadLocation = new Button ( composite_14, SWT.CHECK );
        btnDefaultDownloadLocation.setText ( "Default Download Location" );
        btnDefaultDownloadLocation.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedShare != null )
                {
                    getNode().getShareManager().addShare (
                        selectedShare.getCommunityId(),
                        selectedShare.getMemberId(),
                        selectedShare.getShareName(),
                        selectedShare.getDirectory(),
                        btnDefaultDownloadLocation.getSelection() );

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnDelete = new Button ( composite_14, SWT.NONE );
        btnDelete.setText ( "Delete" );

        new Label ( composite_14, SWT.NONE );

        txtAShareIs = new Text ( composite_14, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL );
        txtAShareIs.setEditable ( false );
        txtAShareIs.setText ( "A Share is a directory or folder on your system, where all "
                              + "files are automatically shared with the community. "
                              + "Any new files copied intoto this directory will automatically "
                              + "be shared. You can move and rename files within a share, "
                              + "and others users will still be able to download these. You can also "
                              + "download new files to Share directories." );
        txtAShareIs.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        btnDelete.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( selectedShare != null )
                {
                    getNode().getShareManager().deleteShare ( selectedShare.getCommunityId(),
                            selectedShare.getMemberId(), selectedShare.getShareName() );

                    if ( selectedCommunity != null && selectedIdentity != null )
                    {
                        setShares ( selectedCommunity.getDig(), selectedIdentity.getId() );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );


        TabItem tbtmDownloadds = new TabItem ( tabFolder, SWT.NONE );
        tbtmDownloadds.setText ( "Downloads" );

        Composite composite_10 = new Composite ( tabFolder, SWT.NONE );
        tbtmDownloadds.setControl ( composite_10 );
        composite_10.setLayout ( new GridLayout ( 1, false ) );

        downloadTableViewer = new TableViewer ( composite_10, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI );
        downloadTableViewer.setContentProvider ( new DownloadContentProvider() );
        downloadTable = downloadTableViewer.getTable();
        downloadTable.setHeaderVisible ( true );
        downloadTable.setLinesVisible ( true );
        downloadTable.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        downloadTableViewer.setSorter ( new DownloadsSorter() );

        TableViewerColumn dlcol0 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol0.getColumn().setText ( "File" );
        dlcol0.getColumn().setWidth ( 200 );
        dlcol0.setLabelProvider ( new DownloadsColumnFileName() );
        dlcol0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 0 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol1 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol1.getColumn().setText ( "Priority" );
        dlcol1.getColumn().setWidth ( 50 );
        dlcol1.setLabelProvider ( new DownloadsColumnPriority() );
        dlcol1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 1 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol2 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol2.getColumn().setText ( "Downloaded Parts" );
        dlcol2.getColumn().setWidth ( 100 );
        dlcol2.setLabelProvider ( new DownloadsColumnDownloaded() );
        dlcol2.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 2 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol3 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol3.getColumn().setText ( "Total Parts" );
        dlcol3.getColumn().setWidth ( 100 );
        dlcol3.setLabelProvider ( new DownloadsColumnTotalFragments() );
        dlcol3.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 3 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol4 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol4.getColumn().setText ( "File Size" );
        dlcol4.getColumn().setWidth ( 200 );
        dlcol4.setLabelProvider ( new DownloadsColumnFileSize() );
        dlcol4.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 4 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol5 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol5.getColumn().setText ( "State" );
        dlcol5.getColumn().setWidth ( 200 );
        dlcol5.setLabelProvider ( new DownloadsColumnState() );
        dlcol5.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 5 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn dlcol6 = new TableViewerColumn ( downloadTableViewer, SWT.NONE );
        dlcol6.getColumn().setText ( "Requested by" );
        dlcol6.getColumn().setWidth ( 200 );
        dlcol6.setLabelProvider ( new DownloadsColumnId() );
        dlcol6.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DownloadsSorter srt = ( DownloadsSorter ) downloadTableViewer.getSorter();
                srt.doSort ( 6 );
                downloadTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Menu menu_4 = new Menu ( downloadTable );
        downloadTable.setMenu ( menu_4 );

        MenuItem changepriority = new MenuItem ( menu_4, SWT.NONE );
        changepriority.setText ( "Change Priority" );
        changepriority.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) downloadTableViewer.getSelection();
                downloadPriorityDialog.open ( sel );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem canceldl = new MenuItem ( menu_4, SWT.NONE );
        canceldl.setText ( "Cancel download" );
        canceldl.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) downloadTableViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                while ( i.hasNext() )
                {
                    RequestFile rf = ( RequestFile ) i.next();
                    getNode().getFileHandler().cancelDownload ( rf );
                    getUserCallback().update ( rf );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TabItem tbtmConnections = new TabItem ( tabFolder, SWT.NONE );
        tbtmConnections.setText ( "Connections" );

        Composite composite_8 = new Composite ( tabFolder, SWT.NONE );
        tbtmConnections.setControl ( composite_8 );
        composite_8.setLayout ( new FillLayout ( SWT.HORIZONTAL ) );

        connectionTableViewer = new TableViewer ( composite_8, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        connectionTable = connectionTableViewer.getTable();
        connectionTable.setHeaderVisible ( true );
        connectionTable.setLinesVisible ( true );
        connectionTableViewer.setContentProvider ( new ConnectionContentProvider() );
        connectionTableViewer.setSorter ( new ConnectionSorter() );


        Menu menu_7 = new Menu ( connectionTable );
        connectionTable.setMenu ( menu_7 );

        MenuItem closecon = new MenuItem ( menu_7, SWT.NONE );
        closecon.setText ( "Close Connection" );
        closecon.addSelectionListener ( new SelectionListener()
        {
            @SuppressWarnings ( "rawtypes" )
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) connectionTableViewer.getSelection();
                Iterator i = sel.iterator();

                while ( i.hasNext() )
                {
                    ConnectionThread ct = ( ConnectionThread ) i.next();
                    ct.stop();
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );


        TableViewerColumn concol00 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol00.getColumn().setText ( "Local" );
        concol00.getColumn().setWidth ( 200 );
        concol00.setLabelProvider ( new ConnectionColumnDest() );
        concol00.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 0 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol0 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol0.getColumn().setText ( "Remote" );
        concol0.getColumn().setWidth ( 200 );
        concol0.setLabelProvider ( new ConnectionColumnId() );
        concol0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 1 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol1 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol1.getColumn().setText ( "Upload" );
        concol1.getColumn().setWidth ( 200 );
        concol1.setLabelProvider ( new ConnectionColumnUpload() );
        concol1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 2 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol2 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol2.getColumn().setText ( "Download" );
        concol2.getColumn().setWidth ( 200 );
        concol2.setLabelProvider ( new ConnectionColumnDownload() );

        concol2.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 3 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol3 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol3.getColumn().setText ( "Time" );
        concol3.getColumn().setWidth ( 200 );
        concol3.setLabelProvider ( new ConnectionColumnTime() );

        concol3.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 4 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol4 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol4.getColumn().setText ( "Last Sent" );
        concol4.getColumn().setWidth ( 200 );
        concol4.setLabelProvider ( new ConnectionColumnLastSent() );

        concol4.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 5 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol5 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol5.getColumn().setText ( "Last Read" );
        concol5.getColumn().setWidth ( 200 );
        concol5.setLabelProvider ( new ConnectionColumnLastRead() );

        concol5.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 6 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol6 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol6.getColumn().setText ( "Pending" );
        concol6.getColumn().setWidth ( 200 );
        concol6.setLabelProvider ( new ConnectionColumnPending() );

        concol6.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 7 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn concol7 = new TableViewerColumn ( connectionTableViewer, SWT.NONE );
        concol7.getColumn().setText ( "Mode" );
        concol7.getColumn().setWidth ( 200 );
        concol7.setLabelProvider ( new ConnectionColumnMode() );

        concol7.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                ConnectionSorter srt = ( ConnectionSorter ) connectionTableViewer.getSorter();
                srt.doSort ( 8 );
                connectionTableViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        bannerText = new Text ( shell, SWT.BORDER );
        bannerText.setEditable ( false );
        bannerText.setLayoutData ( new GridData ( SWT.FILL, SWT.BOTTOM, true, false, 1, 1 ) );
        bannerText.setText ( Wrapper.getLastDevMessage() );

    }

    public Tree getIdentTree()
    {
        return identTree;
    }

    public Label getLblIdentCommunity()
    {
        return lblIdentCommunity;
    }

    public Text getSearchText()
    {
        return searchText;
    }

    public StyledText getPostText()
    {
        return postText;
    }

    public Table getConnectionTable()
    {
        return connectionTable;
    }

    public Table getFileTable()
    {
        return fileTable;
    }

    public TableViewer getFileTableViewer()
    {
        return fileTableViewer;
    }

    public Text getFileSearch()
    {
        return fileSearch;
    }

    public Table getDownloadTable()
    {
        return downloadTable;
    }

    public TableViewer getDownloadTableViewer()
    {
        return downloadTableViewer;
    }

    public Text getBannerText()
    {
        return bannerText;
    }

    public Label getLblVersion()
    {
        return lblVersion;
    }

    public Table getMembershipTable()
    {
        return membershipTable;
    }

    public TableViewer getMembershipTableViewer()
    {
        return membershipTableViewer;
    }

    public Label getLblError()
    {
        return lblError;
    }

    public Combo getShareCombo()
    {
        return shareCombo;
    }

    public ComboViewer getShareComboViewer()
    {
        return shareComboViewer;
    }

    public Combo getComboShareName()
    {
        return comboShareName;
    }

    public ComboViewer getComboShareNameViewer()
    {
        return comboShareNameViewer;
    }

    public Button getBtnDefaultDownloadLocation()
    {
        return btnDefaultDownloadLocation;
    }

}
