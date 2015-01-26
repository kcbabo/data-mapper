package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.TransformType;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.dozer.DozerMapperConfiguration;
import org.jboss.mapper.model.json.JsonModelGenerator;
import org.jboss.mapper.model.xml.XmlModelGenerator;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

/**
 *
 */
public class DataMappingWizard extends Wizard implements INewWizard {

    static final String OBJECT_FACTORY_NAME = "ObjectFactory";
    static final String MAIN_PATH = "src/main/";
    static final String JAVA_PATH = MAIN_PATH + "java/";
    static final String RESOURCES_PATH = MAIN_PATH + "resources/";
    static final String CAMEL_CONFIG_PATH = RESOURCES_PATH + "META-INF/spring/camel-context.xml";
    static final String DEFAULT_DOZER_CONFIG_PATH = "dozerBeanMapping.xml";
    static final String LABEL_PROPERTY = "label";
    static final String TOOL_TIP_PROPERTY = "toolTip";

    private static void populateResources( Shell shell,
                                           IContainer container,
                                           List< IResource > resources ) {
        try {
            for ( final IResource resource : container.members() ) {
                if ( resource instanceof IContainer ) populateResources( shell, ( IContainer ) resource, resources );
                else resources.add( resource );
            }
        } catch ( final Exception e ) {
            Activator.error( shell, e );
        }
    }

    static String selectSchema( final Shell shell,
                                final IProject project,
                                final String schemaType,
                                final Text fileText ) {
        final int flags = JavaElementLabelProvider.SHOW_DEFAULT |
                          JavaElementLabelProvider.SHOW_POST_QUALIFIED |
                          JavaElementLabelProvider.SHOW_ROOT;
        final ElementListSelectionDialog dlg =
            new ElementListSelectionDialog( shell, new JavaElementLabelProvider( flags ) {

                @Override
                public String getText( Object element ) {
                    return super.getText( element ) + " - " + ( ( IResource ) element ).getParent().getFullPath().makeRelative();
                }
            } );
        dlg.setTitle( "Select " + schemaType );
        dlg.setMessage( "Select the " + schemaType + " file for the transformation" );
        dlg.setMatchEmptyString( true );
        dlg.setHelpAvailable( false );
        final List< IResource > resources = new ArrayList<>();
        populateResources( shell, project, resources );
        dlg.setElements( resources.toArray() );
        if ( dlg.open() == Window.OK ) return ( ( IFile ) dlg.getFirstResult() ).getProjectRelativePath().toString();
        return null;
    }

    IProject project;
    ComboViewer projectViewer;
    Text idText;
    IFile dozerConfigFile;
    Text sourceFileText, targetFileText;
    Button sourceFileButton, targetFileButton;
    ComboViewer sourceTypeComboViewer, targetTypeComboViewer;
    File camelConfigFile;
    CamelConfigBuilder camelConfigBuilder;
    Color labelForeground;
    Color textForeground;
    Color comboForeground;

    /**
     *
     */
    public DataMappingWizard() {
        addPage( constructMainPage() );
    }

    private IWizardPage constructMainPage() {
        return new WizardPage( "New Data Mapping", "New Data Mapping", Activator.imageDescriptor( "transform.png" ) ) {

            Text dozerConfigFileText;

            /**
             * {@inheritDoc}
             *
             * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
             */
            @Override
            public void createControl( final Composite parent ) {
                setDescription( "Supply the ID, project, and name for the the new mapping.\n" +
                                "Optionally, supply the source and target files to be mapped." );
                final Composite page = new Composite( parent, SWT.NONE );
                setControl( page );
                page.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
                // Create project controls
                Label label = new Label( page, SWT.NONE );
                labelForeground = label.getForeground();
                label.setText( "Project:" );
                label.setToolTipText( "The project that will contain the mapping file." );
                projectViewer = new ComboViewer( new Combo( page, SWT.READ_ONLY ) );
                projectViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults()
                                                                       .grab( true, false )
                                                                       .span( 2, 1 )
                                                                       .align( SWT.FILL, SWT.CENTER )
                                                                       .create() );
                comboForeground = projectViewer.getCombo().getForeground();
                projectViewer.getCombo().setData( LABEL_PROPERTY, label );
                projectViewer.getCombo().setToolTipText( label.getToolTipText() );
                projectViewer.setLabelProvider( new LabelProvider() {

                    @Override
                    public String getText( final Object element ) {
                        return ( ( IProject ) element ).getName();
                    }
                } );
                projectViewer.add( ResourcesPlugin.getWorkspace().getRoot().getProjects() );
                projectViewer.getCombo().addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( final ModifyEvent event ) {
                        project = ( IProject ) ( ( IStructuredSelection ) projectViewer.getSelection() ).getFirstElement();
                        camelConfigFile = new File( project.getFile( CAMEL_CONFIG_PATH ).getLocationURI() );
                        try {
                            camelConfigBuilder = CamelConfigBuilder.loadConfig( camelConfigFile );
                        } catch ( final Exception e ) {
                            Activator.error( getShell(), e );
                        }
                        validatePage();
                    }
                } );
                // Create ID controls
                label = new Label( page, SWT.NONE );
                label.setText( "ID:" );
                label.setToolTipText( "The transform ID that will be shown in the Fuse editor" );
                idText = new Text( page, SWT.BORDER );
                idText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                     .align( SWT.FILL, SWT.CENTER ).create() );
                textForeground = idText.getForeground();
                idText.setData( LABEL_PROPERTY, label );
                idText.setToolTipText( label.getToolTipText() );
                idText.addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( final ModifyEvent event ) {
                        validatePage();
                    }
                } );
                // Create Dozer config controls
                label = new Label( page, SWT.NONE );
                label.setText( "File name:" );
                dozerConfigFileText = new Text( page, SWT.BORDER );
                dozerConfigFileText.setText( DEFAULT_DOZER_CONFIG_PATH );
                dozerConfigFileText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                                  .align( SWT.FILL, SWT.CENTER ).create() );
                dozerConfigFileText.setData( LABEL_PROPERTY, label );
                dozerConfigFileText.setToolTipText( label.getToolTipText() );
                dozerConfigFileText.addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( final ModifyEvent event ) {
                        validatePage();
                    }
                } );
                // Create source controls
                Group group = new Group( page, SWT.SHADOW_ETCHED_IN );
                Label fileLabel = new Label( group, SWT.NONE );
                sourceFileText = new Text( group, SWT.BORDER );
                sourceFileButton = new Button( group, SWT.NONE );
                Label typeLabel = new Label( group, SWT.NONE );
                sourceTypeComboViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
                createFileControls( group, fileLabel, "Source", sourceFileText, sourceFileButton, typeLabel, sourceTypeComboViewer );
                // Create target controls
                group = new Group( page, SWT.SHADOW_ETCHED_IN );
                fileLabel = new Label( group, SWT.NONE );
                targetFileText = new Text( group, SWT.BORDER );
                targetFileButton = new Button( group, SWT.NONE );
                typeLabel = new Label( group, SWT.NONE );
                targetTypeComboViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
                createFileControls( group, fileLabel, "Target", targetFileText, targetFileButton, typeLabel, targetTypeComboViewer );
                // Set focus to appropriate control
                page.addPaintListener( new PaintListener() {

                    @Override
                    public void paintControl( final PaintEvent event ) {
                        if ( project == null ) projectViewer.getCombo().setFocus();
                        else idText.setFocus();
                        page.removePaintListener( this );
                    }
                } );

                if ( project == null ) validatePage();
                else projectViewer.setSelection( new StructuredSelection( project ) );
            }

            void createFileControls( final Group group,
                                     final Label fileLabel,
                                     final String schemaType,
                                     final Text fileText,
                                     final Button fileButton,
                                     final Label typeLabel,
                                     final ComboViewer typeComboViewer ) {
                group.setLayoutData( GridDataFactory.swtDefaults()
                                                    .grab( true, false )
                                                    .span( 3, 1 )
                                                    .align( SWT.FILL, SWT.CENTER )
                                                    .create() );
                group.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
                group.setText( schemaType + " File" );
                fileLabel.setText( "Name:" );
                fileText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER ).create() );
                fileText.setData( LABEL_PROPERTY, fileLabel );
                fileText.addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( ModifyEvent event ) {
                        validatePage();
                    }
                } );
                fileButton.setText( "..." );
                typeLabel.setText( "Type:" );
                typeComboViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false ).create() );
                typeComboViewer.getCombo().setData( LABEL_PROPERTY, typeLabel );
                typeComboViewer.add( ModelType.values() );
                typeComboViewer.getCombo().addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( final ModifyEvent event ) {
                        validatePage();
                    }
                } );
                fileButton.addSelectionListener( new SelectionAdapter() {

                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final String name = selectSchema( getShell(), project, schemaType, fileText );
                        if ( name != null ) {
                            fileText.setText( name );
                            if ( typeComboViewer.getSelection().isEmpty() ) {
                                final String ext = name.substring( name.lastIndexOf( '.' ) + 1 ).toLowerCase();
                                switch ( ext ) {
                                    case "class":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.CLASS ) );
                                        break;
                                    case "java":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.JAVA ) );
                                        break;
                                    case "json":
                                        try ( InputStream stream = project.getFile( name ).getContents() ) {
                                            char quote = '\0';
                                            final StringBuilder builder = new StringBuilder();
                                            ModelType type = ModelType.JSON;
                                            for ( char chr = (char) stream.read(); chr != -1; chr = (char) stream.read() ) {
                                                // Find quote
                                                if ( quote == '\0' ) {
                                                    if ( chr == '"' || chr == '\'' ) quote = chr;
                                                } else if ( chr == quote ) {
                                                    final String keyword = builder.toString();
                                                    switch ( keyword ) {
                                                        case "$schema":
                                                        case "title":
                                                        case "type":
                                                        case "id":
                                                            type = ModelType.JSON_SCHEMA;
                                                    }
                                                    break;
                                                }
                                                else builder.append( chr );
                                            }
                                            typeComboViewer.setSelection( new StructuredSelection( type ) );
                                        } catch ( IOException | CoreException e ) {
                                            Activator.error( getShell(), e );
                                            typeComboViewer.setSelection( new StructuredSelection( ModelType.JSON ) );
                                        }
                                        break;
                                    case "xml":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.XML ) );
                                        break;
                                    case "xsd":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.XSD ) );
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        validatePage();
                    }
                } );
            }

            void markInvalid( Control control,
                              String toolTip ) {
                control.setData( TOOL_TIP_PROPERTY, control.getToolTipText() );
                control.setToolTipText( toolTip );
                final Color color = getShell().getDisplay().getSystemColor( SWT.COLOR_RED );
                control.setForeground( color );
                final Label label = ( Label ) control.getData( LABEL_PROPERTY );
                label.setToolTipText( toolTip );
                label.setForeground( color );
            }

            void markValid( Control control ) {
                final Object data = control.getData( TOOL_TIP_PROPERTY );
                final String toolTip = data == null ? null : data.toString();
                control.setToolTipText( toolTip );
                control.setForeground( control instanceof Combo ? comboForeground : textForeground );
                final Label label = ( Label ) control.getData( LABEL_PROPERTY );
                label.setToolTipText( toolTip );
                label.setForeground( labelForeground );
            }

            void validatePage() {
                setPageComplete( false );
                if ( project == null ) {
                    sourceFileButton.setEnabled( false );
                    targetFileButton.setEnabled( false );
                    markInvalid( projectViewer.getCombo(), "A project must be selected" );
                    return;
                }
                markValid( projectViewer.getCombo() );
                sourceFileButton.setEnabled( true );
                targetFileButton.setEnabled( true );
                final String id = idText.getText().trim();
                if ( id.isEmpty() ) {
                    markInvalid( idText, "A mapping ID must be supplied" );
                    return;
                }
                final StringCharacterIterator iter = new StringCharacterIterator( id );
                for ( char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next() ) {
                    if ( !Character.isUnicodeIdentifierPart( chr ) ) {
                        markInvalid( idText, "The mapping ID contains an illegal character" );
                        return;
                    }
                }
                for ( final CamelEndpointFactoryBean bean : camelConfigBuilder.getCamelContext().getEndpoint() ) {
                    if ( id.equalsIgnoreCase( bean.getId() ) ) {
                        markInvalid( idText, "A mapping with the supplied ID already exists" );
                        return;
                    }
                }
                markValid( idText );
                String path = dozerConfigFileText.getText();
                if ( path.isEmpty() ) {
                    dozerConfigFile = null;
                    markInvalid( dozerConfigFileText, "The name of the mapping file must be supplied" );
                    return;
                }
                markValid( dozerConfigFileText );
                if ( !path.toLowerCase().endsWith( ".xml" ) ) path = path + ".xml";
                dozerConfigFile = project.getFile( RESOURCES_PATH + path );
                final String sourceFileName = sourceFileText.getText().trim();
                final String targetFileName = targetFileText.getText().trim();
                if ( sourceFileName.isEmpty() && targetFileName.isEmpty() ) {
                    markValid( sourceFileText );
                    markValid( targetFileText );
                    setPageComplete( true );
                    return;
                }
                if ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) {
                    if ( project.findMember( sourceFileName ) == null ) {
                        markInvalid( sourceFileText, "The supplied source file does not exist" );
                        return;
                    }
                    markValid( sourceFileText );
                    if ( project.findMember( targetFileName ) == null ) {
                        markInvalid( targetFileText, "The supplied target file does not exist" );
                        return;
                    }
                    markValid( targetFileText );
                    if ( sourceTypeComboViewer.getSelection().isEmpty() ) {
                        markInvalid( sourceTypeComboViewer.getCombo(), "A source type must be selected" );
                        return;
                    }
                    markValid( sourceTypeComboViewer.getCombo() );
                    if ( targetTypeComboViewer.getSelection().isEmpty() ) {
                        markInvalid( targetTypeComboViewer.getCombo(), "The target type must be selected" );
                        return;
                    }
                    markValid( targetTypeComboViewer.getCombo() );
                    setPageComplete( true );
                    return;
                }
                if ( sourceFileName.isEmpty() ) {
                    markInvalid( sourceFileText, "A source file must be selected since a target file has been selected" );
                    markValid( targetFileText );
                }
                else {
                    markValid( sourceFileText );
                    markInvalid( targetFileText, "A target file must be selected since a source file has been selected" );
                }
            }
        };
    }

    private String generateModel( final String fileName,
                                  final ModelType type ) throws Exception {
        // Build class name from file name
        final StringBuilder className = new StringBuilder();
        final StringCharacterIterator iter =
            new StringCharacterIterator( fileName.substring( fileName.lastIndexOf( '/' ) + 1, fileName.lastIndexOf( '.' ) ) );
        boolean wordStart = true;
        for ( char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next() ) {
            if ( className.length() == 0 ) {
                if ( Character.isJavaIdentifierStart( chr ) ) {
                    className.append( wordStart ? Character.toUpperCase( chr ) : chr );
                    wordStart = false;
                }
            } else if ( Character.isJavaIdentifierPart( chr ) ) {
                className.append( wordStart ? Character.toUpperCase( chr ) : chr );
                wordStart = false;
            } else wordStart = true;
        }
        // Build package name from class name
        int sequencer = 1;
        String pkgName = className.toString();
        while ( project.exists( new Path( JAVA_PATH + pkgName ) ) ) {
            pkgName = className.toString() + sequencer++;
        }
        pkgName = pkgName.toLowerCase();
        // Generate model
        final File targetClassesFolder = new File( project.getFolder( JAVA_PATH ).getLocationURI() );
        switch ( type ) {
            case CLASS: {
                final IResource resource = project.findMember( fileName );
                if ( resource != null ) {
                    final IClassFile file = ( IClassFile ) JavaCore.create( project.findMember( fileName ) );
                    if ( file != null ) return pkgName + "." + file.getType().getFullyQualifiedName();
                }
                return null;
            }
            case JAVA: {
                final IResource resource = project.findMember( fileName );
                if ( resource != null ) {
                    final ICompilationUnit file = ( ICompilationUnit ) JavaCore.create( project.findMember( fileName ) );
                    if ( file != null ) {
                        final IType[] types = file.getTypes();
                        if ( types.length > 0 ) return types[ 0 ].getFullyQualifiedName();
                    }
                }
                return null;
            }
            case JSON: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromInstance( className.toString(),
                                                pkgName,
                                                project.findMember( fileName ).getLocationURI().toURL(),
                                                targetClassesFolder );
                return pkgName + "." + className;
            }
            case JSON_SCHEMA: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromSchema( className.toString(),
                                              pkgName,
                                              project.findMember( fileName ).getLocationURI().toURL(),
                                              targetClassesFolder );
                return pkgName + "." + className;
            }
            case XSD: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final JCodeModel model = generator.generateFromSchema( new File( project.findMember( fileName ).getLocationURI() ),
                                                                       pkgName,
                                                                       targetClassesFolder );
                final String modelClass = selectModelClass( model );
                if ( modelClass != null ) { return modelClass; }
                return null;
            }
            case XML: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final File schemaPath = new File( project.getFile( fileName + ".xsd" ).getLocationURI() );
                final JCodeModel model = generator.generateFromInstance( new File( project.findMember( fileName ).getLocationURI() ),
                                                                         schemaPath,
                                                                         pkgName,
                                                                         targetClassesFolder );
                final String modelClass = selectModelClass( model );
                if ( modelClass != null ) { return modelClass; }
                return null;
            }
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init( final IWorkbench workbench,
                      final IStructuredSelection selection ) {
        final IStructuredSelection resourceSelection =
            ( IStructuredSelection ) workbench.getActiveWorkbenchWindow().getSelectionService().getSelection( "org.eclipse.ui.navigator.ProjectExplorer" );
        if ( resourceSelection == null ) return;
        if ( resourceSelection.size() != 1 ) {
            final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            if ( projects.length == 1 ) project = projects[ 0 ];
            return;
        }
        project =
            ( ( IResource ) ( ( IAdaptable ) resourceSelection.getFirstElement() ).getAdapter( IResource.class ) ).getProject();
        if ( project != null ) dozerConfigFile = project.getFile( RESOURCES_PATH + DEFAULT_DOZER_CONFIG_PATH );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        // Save Dozer config
        if ( dozerConfigFile.exists() && !MessageDialog.openConfirm( getShell(), "Confirm", "Overwrite existing file?" ) )
            return false;
        final MapperConfiguration dozerConfigBuilder = DozerMapperConfiguration.newConfig();
        final File newFile = new File( dozerConfigFile.getLocationURI() );
        if ( !newFile.getParentFile().exists() ) {
            newFile.getParentFile().mkdirs();
        }
        try ( FileOutputStream dozerConfigStream = new FileOutputStream( newFile ) ) {
            final String sourceFileName = sourceFileText.getText().trim();
            final String targetFileName = targetFileText.getText().trim();
            if ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) {
                // Generate models
                final ModelType sourceType =
                    ( ModelType ) ( ( IStructuredSelection ) sourceTypeComboViewer.getSelection() ).getFirstElement();
                final String sourceClassName = generateModel( sourceFileName, sourceType );
                final ModelType targetType =
                    ( ModelType ) ( ( IStructuredSelection ) targetTypeComboViewer.getSelection() ).getFirstElement();
                final String targetClassName = generateModel( targetFileName, targetType );
                // Update Camel config
                final IPath resourcesPath = project.getFolder( RESOURCES_PATH ).getFullPath();
                camelConfigBuilder.addTransformation( idText.getText(),
                                                      dozerConfigFile.getFullPath().makeRelativeTo( resourcesPath ).toString(),
                                                      sourceType.transformType, sourceClassName,
                                                      targetType.transformType, targetClassName );
                try ( FileOutputStream camelConfigStream = new FileOutputStream( camelConfigFile ) ) {
                    camelConfigBuilder.saveConfig( camelConfigStream );
                } catch ( final Exception e ) {
                    Activator.error( getShell(), e );
                    return false;
                }
                dozerConfigBuilder.addClassMapping( sourceClassName, targetClassName );
            }
            dozerConfigBuilder.saveConfig( dozerConfigStream );
            project.refreshLocal( IProject.DEPTH_INFINITE, null );
            // Ensure build of Java classes has completed
            Job.getJobManager().join( ResourcesPlugin.FAMILY_AUTO_BUILD, null );
            // Open mapping editor
            final IEditorDescriptor desc =
                PlatformUI.getWorkbench().getEditorRegistry().getEditors( dozerConfigFile.getName(),
                                                                          Platform.getContentTypeManager().getContentType( DozerConfigContentTypeDescriber.ID ) )[ 0 ];
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor( new FileEditorInput( dozerConfigFile ),
                                                                                             desc.getId() );
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
            return false;
        }
        return true;
    }

    private String selectModelClass( final JCodeModel model ) {
        for ( final Iterator< JPackage > pkgIter = model.packages(); pkgIter.hasNext(); ) {
            final JPackage pkg = pkgIter.next();
            for ( final Iterator< JDefinedClass > classIter = pkg.classes(); classIter.hasNext(); ) {
                // TODO this only works when a single top-level class exists; fix after issue #33 is fixed
                final JDefinedClass definedClass = classIter.next();
                if ( OBJECT_FACTORY_NAME.equals( definedClass.name() ) ) continue;
                return definedClass.fullName();
            }
        }
        return null;
    }

    enum ModelType {

        CLASS( "Java Class", TransformType.JAVA ),
        JAVA( "Java Source", TransformType.JAVA ),
        JSON( "JSON", TransformType.JSON ),
        JSON_SCHEMA( "JSON Schema", TransformType.JSON ),
        XML( "XML", TransformType.XML ),
        XSD( "XSD", TransformType.XML );

        final String text;
        final TransformType transformType;

        private ModelType( final String text,
                           final TransformType transformType ) {
            this.text = text;
            this.transformType = transformType;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
