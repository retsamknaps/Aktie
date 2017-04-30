package aktie.gui.pm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.lucene.search.Sort;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.data.CObj;
import aktie.gui.CObjListArrayElement;
import aktie.gui.CObjListGetter;
import aktie.gui.NewPostDialog;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDate;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.index.CObjList;

public class PMTable extends CObjListTable<CObjListArrayElement>
{
    private PMTab tab;
    private SimpleDateFormat dateformat;
    private final String highlightKey;

    public PMTable ( Composite composite, PMTab tab )
    {
        super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

        this.tab = tab;

        dateformat = new SimpleDateFormat ( "d MMM yyyy HH:mm z" );

        highlightKey = CObj.PRV_TEMP_NEWPOSTS;

        setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

        setInputProvider ( new PMTableInputProvider ( tab ) );

        AktieTableViewerColumn<CObjList, CObjListGetter> sortColumn;

        addColumn ( "Sender", 100, new CObjListTableCellLabelProviderTypeDisplayName ( true, highlightKey ) );
        sortColumn = addColumn ( "Date", 100, new CObjListTableCellLabelProviderTypeDate ( CObj.CREATEDON, true, highlightKey ) );
        addColumn ( "Subject", 300, new CObjListTableCellLabelProviderTypeString ( CObj.SUBJECT, true, highlightKey ) );

        getTableViewer().setSortColumn ( sortColumn, true );

        getTableViewer().addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @SuppressWarnings ( "rawtypes" )
            @Override
            public void selectionChanged ( SelectionChangedEvent s )
            {
                IStructuredSelection sel = ( IStructuredSelection ) s.getSelection();
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement selm = ( CObjListArrayElement ) selo;
                        CObj msg = selm.getCObj();

                        if ( msg != null )
                        {
                            PMTable.this.tab.getIdentityModel().clearBlueMessages ( msg );
                            PMTable.this.tab.getTreeViewer().refresh ( true );
                            Long np = msg.getPrivateNumber ( highlightKey );

                            if ( np != null && !np.equals ( 0L ) )
                            {
                                msg.pushPrivateNumber ( highlightKey, 0L );

                                try
                                {
                                    PMTable.this.tab.getSWTApp().getNode().getIndex().index ( msg );
                                }

                                catch ( IOException e1 )
                                {
                                    e1.printStackTrace();
                                }

                            }

                            String pfrom = msg.getString ( CObj.CREATOR );
                            String pto = msg.getPrivate ( CObj.PRV_RECIPIENT );

                            if ( pfrom != null && pto != null )
                            {
                                StringBuilder sb = new StringBuilder();
                                sb.append ( "FROM: " );
                                sb.append ( PMTable.this.tab.getSWTApp().getIdCache().getName ( pfrom ) );
                                sb.append ( "\n" );
                                sb.append ( "TO:   " );
                                sb.append ( PMTable.this.tab.getSWTApp().getIdCache().getName ( pto ) );
                                sb.append ( "\n" );
                                sb.append ( "DATE: " );
                                Long co = msg.getPrivateNumber ( CObj.CREATEDON );

                                if ( co != null )
                                {
                                    sb.append ( dateformat.format ( new Date ( co ) ) );
                                }

                                sb.append ( "\n" );
                                sb.append ( "SUBJ: " );
                                sb.append ( msg.getPrivate ( CObj.SUBJECT ) );
                                sb.append ( "\n======================================\n" );

                                String bdy = msg.getPrivate ( CObj.BODY );

                                if ( bdy != null )
                                {
                                    sb.append ( bdy );
                                }

                                String prttxt = NewPostDialog.formatDisplay ( sb.toString(), false );
                                PMTable.this.tab.getStyledText().setText ( prttxt );
                            }

                        }

                    }

                }

            }

        } );

    }


    private class PMTableInputProvider extends CObjListTableInputProvider
    {
        private PMTab tab;

        public PMTableInputProvider ( PMTab tab )
        {
            this.tab = tab;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            tab.getCurrentMessage();

            CObj currentMessage = tab.getCurrentMessage();

            if ( currentMessage == null )
            {
                return null;
            }

            String messageID = currentMessage.getPrivate ( CObj.PRV_MSG_ID );

            CObjList list = null;

            if ( messageID != null )
            {
                list = tab.getSWTApp().getNode().getIndex().getDecodedPrvMessages ( messageID, sort );
            }

            return list;
        }

    }

}
