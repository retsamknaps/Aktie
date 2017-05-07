package aktie.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProvider;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDate;
import aktie.gui.table.CObjListTableCellLabelProviderTypeLong;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;

import aktie.index.CObjList;

public class PostsTable extends CObjListTable<CObjListArrayElement>
{

    public static final long MAX_PREVIEW_FILE_SIZE = 80L * 1024L * 1024L;

    private final SWTApp app;
    private CObj displayedPost = null;

    private final String highlightKey;

    public PostsTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

        this.app = app;
        highlightKey = CObj.PRV_TEMP_NEWPOSTS;

        setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

        setInputProvider ( new PostsTableInputProvider ( app ) );

        AktieTableViewerColumn<CObjList, CObjListGetter> column;

        addColumn ( "Identity", 100,
                    new CObjListTableCellLabelProviderTypeString ( CObj.CREATOR_NAME, false, highlightKey ) );

        addColumn ( "Rank", 20,
                    new CObjListTableCellLabelProviderTypeLong ( CObj.PRV_USER_RANK, true, highlightKey ) );

        addColumn ( "Subject", 300,
                    new CObjListTableCellLabelProviderTypeString ( CObj.SUBJECT, false, highlightKey ) );

        column = addColumn ( "Date", 100,
                             new CObjListTableCellLabelProviderTypeDate ( CObj.CREATEDON, false, highlightKey ) );
        getTableViewer().setSortColumn ( column, true );

        addColumn ( "File", 100,
                    new CObjListTableCellLabelProviderTypeString ( CObj.NAME, false, highlightKey ) );

        addColumn ( "Preview", 100,
                    new CObjListTableCellLabelProviderTypeString ( CObj.PRV_NAME, false, highlightKey ) );

        addColumn ( "Local File", 100,
                    new PostsTableFileCellLabelProvider ( app, CObj.LOCALFILE, highlightKey ) );

        getTableViewer().addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged ( SelectionChangedEvent e )
            {
                ISelection selection = e.getSelection();

                if ( selection != null && selection instanceof IStructuredSelection )
                {
                    IStructuredSelection structuredSelection = ( IStructuredSelection ) selection;

                    if ( structuredSelection.size() > 0 )
                    {
                        Object selectedObject = structuredSelection.getFirstElement();

                        if ( selectedObject instanceof CObjListArrayElement )
                        {
                            displayedPost = ( ( CObjListArrayElement ) selectedObject ).getCObj();

                            if ( displayedPost != null )
                            {
                                // Mark the post as read
                                displayedPost.pushPrivateNumber ( highlightKey, 0L );

                                try
                                {
                                    PostsTable.this.app.getNode().getIndex().index ( displayedPost );
                                }

                                catch ( IOException e1 )
                                {
                                    e1.printStackTrace();
                                }

                                PostsTable.this.app.postSearch();

                                String displayFormattedPost = getPostString ( displayedPost );
                                displayFormattedPost = NewPostDialog.formatDisplay ( displayFormattedPost, false ) + "\n==========================\n=";
                                PostsTable.this.app.getAnimator().update ( null, 0, 0, 10, 10 );
                                PostsTable.this.app.getPostText().setText ( displayFormattedPost );

                                //String comid, String wdig, String pdig
                                String communityID = displayedPost.getString ( CObj.COMMUNITYID );

                                String previewFileDigest = displayedPost.getString ( CObj.PRV_FILEDIGEST );
                                String previewFragDigest = displayedPost.getString ( CObj.PRV_FRAGDIGEST );
                                Long previewFileSize = displayedPost.getNumber ( CObj.PRV_FILESIZE );

                                File previewFile = getPreviewHasFile ( communityID, previewFileDigest, previewFragDigest, previewFileSize );

                                if ( previewFile != null )
                                {
                                    PostsTable.this.addImage ( previewFile.getPath(), displayFormattedPost.length() - 1 );
                                    return;
                                }

                                // If there was no preview attached, there could be just an image attached as file
                                String fileDigest = displayedPost.getString ( CObj.FILEDIGEST );
                                String fragDigest = displayedPost.getString ( CObj.FRAGDIGEST );
                                Long fileSize = displayedPost.getNumber ( CObj.FILESIZE );

                                File file = getPreviewHasFile ( communityID, fileDigest, fragDigest, fileSize );

                                if ( file != null )
                                {
                                    PostsTable.this.addImage ( file.getPath(), displayFormattedPost.length() - 1 );
                                    return;

                                }

                                // Otherwise, we cannot add an image.
                                PostsTable.this.addImage ( ( String ) null, 0 );

                            }

                        }

                    }

                }

            }

        } );

    }

    private String getPostString ( CObj pst )
    {
        StringBuilder msg = new StringBuilder();

        if ( pst != null )
        {
            String subj = pst.getString ( CObj.SUBJECT );
            String body = pst.getText ( CObj.BODY );
            String auth = pst.getString ( CObj.CREATOR_NAME );
            Long seq = pst.getNumber ( CObj.SEQNUM );
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

            msg.append ( "SEQN: " );

            if ( seq != null )
            {
                msg.append ( seq );
            }

            msg.append ( "\n" );

            msg.append ( "--------------------------------------------\n" );

            Set<String> fld = pst.listFields();

            for ( String fid : fld )
            {
                CObj f = app.getNode().getIndex().getByDig ( fid );

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
                                                         app.getIdCache().getName ( pst.getString ( CObj.CREATOR ) ),
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

    private File getPreviewHasFile ( String comid, String wdig, String pdig, Long fsize )
    {
        File file = null;

        if ( comid != null && wdig != null && pdig != null &&
                fsize != null && fsize < MAX_PREVIEW_FILE_SIZE )
        {
            CObjList clst = app.getNode().getIndex().getMyHasFiles ( comid, wdig, pdig );

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

    private void addImage ( String s, int offset )
    {
        if ( s != null )
        {
            File f = new File ( s );

            if ( f.exists() )
            {
                try
                {
                    ImageLoader loader = new ImageLoader();
                    loader.load ( new FileInputStream ( f ) );
                    addImage ( loader, offset );
                    return;
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        app.getAnimator().update ( null, 0, 0, 10, 10 );

    }

    private static final int MARGIN = 5;
    private void addImage ( ImageLoader image, int offset )
    {
        StyleRange style = new StyleRange ();
        style.start = offset;
        style.length = 1;
        style.data = image;

        int w = image.data[0].width;
        int h = image.data[0].height;

        int ascent = 2 * h / 3;
        int descent = h - ascent;

        app.setPreviewResize ( true );
        style.metrics = new GlyphMetrics ( ascent + MARGIN,
                                           descent + MARGIN, w + 2 * MARGIN );
        StyledText pt = app.getPostText();

        if ( !pt.isDisposed() )
        {
            pt.setStyleRange ( style );
        }

    }

    private class PostsTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;

        public PostsTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            CObj selectedCommunity = app.getSelectedCommunity();

            CObjList list = null;

            if ( selectedCommunity != null )
            {
                String search = app.getPostsSearchText().getText();
                CObj advancedQuery = app.getAdvancedPostsQuery();


                if ( advancedQuery == null )
                {
                    list = app.getNode().getIndex().searchPosts ( selectedCommunity.getDig(), search, sort );
                }

                else
                {
                    List<CObj> queryList = new LinkedList<CObj>();
                    queryList.add ( advancedQuery );
                    list = app.getNode().getIndex().searchPostsQuery ( queryList, sort );
                }

            }

            return list;
        }

    }

    private class PostsTableFileCellLabelProvider extends CObjListTableCellLabelProvider
    {
        private SWTApp app;

        public PostsTableFileCellLabelProvider ( SWTApp app, String key, String highlightKey )
        {
            super ( key, true, SortField.Type.STRING, highlightKey );
            this.app = app;
        }

        @Override
        public String getFormattedAttribute ( CObj post )
        {
            if ( post != null )
            {

                String localFile = post.getPrivate ( CObj.LOCALFILE );

                // FIXME: This should be done somewhere else
                // and less regular, e.g. only when we have a new or changed local file
                // we could look for matching posts and change the local file attribute
                if ( localFile == null )
                {

                    String communityID = post.getString ( CObj.COMMUNITYID );
                    String fileDigest = post.getString ( CObj.FILEDIGEST );
                    String fragDigest = post.getString ( CObj.FRAGDIGEST );

                    if ( communityID != null && fileDigest != null && fragDigest != null )
                    {
                        CObjList clst = app.getNode().getIndex().getMyHasFiles ( communityID, fileDigest, fragDigest );

                        if ( clst.size() > 0 )
                        {
                            try
                            {
                                CObj hr = clst.get ( 0 );
                                localFile = hr.getPrivate ( CObj.LOCALFILE );

                                if ( localFile != null )
                                {
                                    post.pushPrivate ( CObj.LOCALFILE, localFile );
                                    return localFile;
                                }

                            }

                            catch ( Exception e )
                            {
                            }

                        }

                        clst.close();

                    }

                }

                else if ( localFile != null )
                {
                    return localFile;
                }

            }

            return "";
        }

    }

}
