package org.jboss.tools.ws.creation.core.commands;

import java.io.File;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.utils.JBossWSCreationUtils;

public class ImplementationClassCreationCommand extends
		AbstractDataModelOperation {

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	protected static final String TAB = "\t"; //$NON-NLS-1$
	protected static final String SPACE = " "; //$NON-NLS-1$
	protected static final String DOT = "."; //$NON-NLS-1$
	protected static final String COMMA = ","; //$NON-NLS-1$
	protected static final String SEMICOLON = ";"; //$NON-NLS-1$
	protected static final String POUND = "#"; //$NON-NLS-1$
	protected static final String OPEN_PAR = "("; //$NON-NLS-1$
	protected static final String CLOSE_PAR = ")"; //$NON-NLS-1$
	protected static final String OPEN_BRA = "{"; //$NON-NLS-1$
	protected static final String CLOSE_BRA = "}"; //$NON-NLS-1$
	protected static final String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$

	protected static final String JAVA_LANG_OBJECT = "java.lang.Object"; //$NON-NLS-1$
	protected static final String PACKAGE = "package "; //$NON-NLS-1$
	protected static final String CLASS = "class "; //$NON-NLS-1$
	protected static final String IMPORT = "import "; //$NON-NLS-1$
	protected static final String EXTENDS = "extends "; //$NON-NLS-1$
	protected static final String IMPLEMENTS = "implements "; //$NON-NLS-1$
	protected static final String THROWS = "throws "; //$NON-NLS-1$
	protected static final String SUPER = "super"; //$NON-NLS-1$
	protected static final String PUBLIC = "public "; //$NON-NLS-1$
	protected static final String PROTECTED = "protected "; //$NON-NLS-1$
	protected static final String PRIVATE = "private "; //$NON-NLS-1$
	protected static final String STATIC = "static "; //$NON-NLS-1$
	protected static final String ABSTRACT = "abstract "; //$NON-NLS-1$
	protected static final String FINAL = "final "; //$NON-NLS-1$
	protected static final String VOID = "void"; //$NON-NLS-1$
	protected static final String INT = "int"; //$NON-NLS-1$
	protected static final String BOOLEAN = "boolean"; //$NON-NLS-1$
	protected static final String MAIN_METHOD = "\tpublic static void main(String[] args) {"; //$NON-NLS-1$
	protected static final String TODO_COMMENT = "\t\t// TODO Auto-generated method stub"; //$NON-NLS-1$
	protected static final String RETURN_NULL = "\t\treturn null;"; //$NON-NLS-1$
	protected static final String RETURN_0 = "\t\treturn 0;"; //$NON-NLS-1$
	protected static final String RETURN_FALSE = "\t\treturn false;"; //$NON-NLS-1$
	protected static final String RESOURCE_FOLDER = "src";
	
	protected static final String PREFIX_JAXWS_ANNOTATION_CLASS = "javax.jws";
	protected static final String CLASS_LOGGER = "Logger";
	protected static final String SUFFIX_PACKAGENAME_IMPL = "impl";
	protected static final String DEFAULT_CU_SUFFIX = ".java";
	
	private ServiceModel model;
	private List<String> importStatements;
	private IWorkspaceRoot fWorkspaceRoot;
	private IJavaProject javaProject;
	
	public ImplementationClassCreationCommand(ServiceModel model){
		this.model = model;
		fWorkspaceRoot= ResourcesPlugin.getWorkspace().getRoot();	
		
	}
	
	
	
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		
		
		
		IFile inFile = getServiceInterfaceFile(model.getPortTypes().get(0));
		
		try {
			//IFile implJavaFile = createEmptyImplJavaFile("Test");
			//so far, it only generate implementation for first port type, neet to generate impl class for all
			//port type interfaces
			generateImplClass(model.getPortTypes().get(0));
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Status.OK_STATUS;
	}
	
	private String getImplPackageName(){
		return model.getCustomPackage() + ".impl";		
	}
	
	private IPackageFragmentRoot getPackageFragmentRoot(){
		String str = model.getWebProjectName() + File.separator + RESOURCE_FOLDER;
		IPath path= new Path(str);
		IResource res= fWorkspaceRoot.findMember(path);
		IProject prj = res.getProject();
		IJavaProject javaPrj = JavaCore.create(prj);
		return javaPrj.getPackageFragmentRoot(res);
		
	}
	
	private String getCompilationUnitName(String portTypeName){

		return getImplClassName(portTypeName) + DEFAULT_CU_SUFFIX;
	}
	

	private String getImplClassName(String portTypeName){
		String firstLetter = portTypeName.substring(0,1);
		String implClsName = firstLetter.toUpperCase() + portTypeName.substring(1);
		implClsName = implClsName + "Impl";
		return implClsName;
	}
	
	/*private CompilationUnit createASTForImplementation(String portTypeName) throws JavaModelException {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= root.getPackageFragment(getImplPackageName()); //$NON-NLS-1$
		
		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, null);
		} 
		
		String cuName= getCompilationUnitName(portTypeName);
		ICompilationUnit parentCU= pack.createCompilationUnit(cuName, "", false, null); //$NON-NLS-1$
		// create a working copy with a new owner
		
		boolean needsSave= true;
		parentCU.becomeWorkingCopy(null); // cu is now a (primary) working copy
		
		
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(parentCU);
		parser.setResolveBindings(false);
		parser.setFocalPosition(0);
		return (CompilationUnit) parser.createAST(null);
	}*/

/*	private IFile createEmptyImplJavaFile(String portTypeName) throws CoreException{
		
		
		IFile inFile = getServiceInterfaceFile(portTypeName);
		if(inFile != null && inFile.exists()){
			IFolder implFolder = inFile.getParent().getFolder(new Path("impl"));
			if(!implFolder.exists()){
					implFolder.create(true, true, null);
			}
			IFile implJava = implFolder.getFile(model.getServiceNames().get(0) + ".java");
			return implJava;
		}
		return null;
	}*/
	
	protected void generateImplClass(String portTypeName/*, IFile implJavaFile*/) throws CoreException, MalformedTreeException, BadLocationException{

		CompilationUnit cu = getCompilationUnitForInterface(portTypeName);
		Object obj = cu.imports();
		List<ImportDeclaration> imports = getImportsWithoutJaxwsAnnotation(cu);
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= root.getPackageFragment(getImplPackageName()); //$NON-NLS-1$
		
		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, null);
		} 
		
		String cuName= getCompilationUnitName(portTypeName);
		ICompilationUnit parentCU= pack.createCompilationUnit(cuName, "", true, null); //$NON-NLS-1$
		// create a working copy with a new owner
		
		parentCU.becomeWorkingCopy(null); // cu is now a (primary) working copy
		
		
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(parentCU);
		parser.setResolveBindings(false);
		parser.setFocalPosition(0);
		
		CompilationUnit implCu = (CompilationUnit) parser.createAST(null);
		AST ast = implCu.getAST();

		//creation of a Document and ASTRewrite
		String source = parentCU.getBuffer().getContents();
		Document document = new Document(source);
		
		ASTRewrite rewrite = ASTRewrite.create(implCu.getAST());
		implCu.recordModifications();
		
		//start to add content implementation class
		
		
		//add package declaration for impl class:
		PackageDeclaration implPackage = ast.newPackageDeclaration();
		implPackage.setName(ast.newName(pack.getElementName()));
		implCu.setPackage(implPackage);
		
		//add imports for implementation class
		for(ImportDeclaration id: imports){
			ImportDeclaration newId = ast.newImportDeclaration();
			newId.setName(ast.newName(id.getName().getFullyQualifiedName()));
			implCu.imports().add(newId);
		}
		
		//import port type interface and jboss Logger
		ImportDeclaration id = ast.newImportDeclaration();
		QualifiedName portTypeImport = ast.newQualifiedName(ast.newName(cu
				.getPackage().getName().getFullyQualifiedName()), ast
				.newSimpleName(portTypeName));
		id.setName(portTypeImport);
		implCu.imports().add(id);
		id = ast.newImportDeclaration();
		id.setName(ast.newName("org.jboss.logging.Logger"));
		implCu.imports().add(id);
		
		//add class declaration 
		TypeDeclaration type = ast.newTypeDeclaration();
		type.setInterface(false);
		type.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		type.setName(ast.newSimpleName(getImplClassName(portTypeName)));
		type.superInterfaceTypes().add(ast.newSimpleType(ast.newName(portTypeName)));
		
		//add Logger variable declaration
		//createLoggerField(ast, type);
		
		
		//add method implementation
		TypeDeclaration inTD = (TypeDeclaration)cu.types().get(0);
		//firstly, get all methods that declared in Interface class and then add corresponding methods to 
		// the impl class
		MethodDeclaration[] methods = inTD.getMethods();
		for(int i = 0; i < methods.length; i++){
			MethodDeclaration newMethod = createMethodForImplClass(ast, methods[i]);
			type.bodyDeclarations().add(newMethod);
		}
		
		
		implCu.types().add(type);
		
		
		//try to save the modification 
		TextEdit edits = implCu.rewrite(document, parentCU.getJavaProject().getOptions(true));
		edits.apply(document);
		String newSource = document.get();
		parentCU.getBuffer().setContents(newSource);
		parentCU.reconcile(ICompilationUnit.NO_AST, false, null, null);
		parentCU.commitWorkingCopy(true, null);
		parentCU.discardWorkingCopy();
		
		
/*		String content = implCu.toString();
		
		if(implJavaFile.exists()){
			implJavaFile.delete(true, null);
		}
		
		implJavaFile.create(new StringBufferInputStream(content), true, null);*/
		
	}
	
	protected FieldDeclaration createLoggerField(AST ast, TypeDeclaration type){
		//for now, have no idea how to generate a field like:
		// private static Logger log = Logger.getLooger(TestEdnpointImpl.class);
		//TODO
		
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName("log"));
		Initializer clsAccesss = ast.newInitializer();
		Block clsAccessBlk = ast.newBlock();
		FieldAccess fa = ast.newFieldAccess();
		fa.setExpression(ast.newSimpleName("Test"));
		fa.setName(ast.newSimpleName("class"));
		clsAccessBlk.statements().add(ast.newExpressionStatement(fa));
		clsAccesss.setBody(clsAccessBlk);
		MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(ast.newSimpleName("Logger"));
		mi.setName(ast.newSimpleName("getLogger"));
		mi.arguments().add(fa);
		vdf.setInitializer(mi);
		
		type.bodyDeclarations().add(vdf);
		
		/*SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		svd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		svd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		svd.setType(ast.newSimpleType(ast.newSimpleName(CLASS_LOGGER)));
		svd.setName(ast.newSimpleName("log"));
		Initializer initializer = ast.newInitializer();
		Block initBlock = ast.newBlock();
		
		MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(ast.newName("Logger"));
		mi.setName(ast.newSimpleName("getLogger"));
		
		
		MethodInvocation invokCls = ast.newMethodInvocation();
		invokCls.setExpression((ast.newSimpleName("TestImpl"));
		invokCls.setName(ast.newSimpleName("class"));
		mi.arguments().add(invokCls.getExpression();
		svd.setInitializer(ast.newExpressionStatement(mi).getExpression());*/
		return null;
	}
	
	protected MethodDeclaration createMethodForImplClass(AST ast, MethodDeclaration inMethod){
		
		MethodDeclaration md = ast.newMethodDeclaration();
		md.setConstructor(false);
		List modifiers = md.modifiers();
		modifiers.add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		md.setName(ast.newSimpleName(inMethod.getName().getFullyQualifiedName()));
		
		SimpleType sType = (SimpleType)inMethod.getReturnType2();
		Name sTypeName = sType.getName();		
		md.setReturnType2(ast.newSimpleType(ast.newName(sTypeName.getFullyQualifiedName())));
		
		List parameters = inMethod.parameters();;
		for(Object obj: parameters){
			SingleVariableDeclaration implSvd = ast.newSingleVariableDeclaration();
			SingleVariableDeclaration svd = (SingleVariableDeclaration)obj;
			implSvd.setName(ast.newSimpleName(svd.getName().getFullyQualifiedName()));
			implSvd.setType(copyTypeFromOtherASTNode(ast, svd.getType()));
			md.parameters().add(implSvd);
		}
		
		//create method body
		Block block = ast.newBlock();
		Type returnType = inMethod.getReturnType2();
		ReturnStatement rs = ast.newReturnStatement();
		String typeName = ((SimpleType)returnType).getName().getFullyQualifiedName();			
		if(returnType.isPrimitiveType() && returnType.isSimpleType()){
				NumberLiteral nl = ast.newNumberLiteral();
				nl.setToken("0");
				rs.setExpression(nl);
				
		}else if("String".equals(typeName)){			
			StringLiteral sl = ast.newStringLiteral();
			sl.setLiteralValue("");
			rs.setExpression(sl);
			
		}else{
			rs.setExpression(ast.newNullLiteral());
		}
		block.statements().add(rs);

		md.setBody(block);
		
		return md;
	}
	
	protected Name copyQualifiedName(AST ast, QualifiedName qname){
		String fullQName = qname.getFullyQualifiedName();
		StringTokenizer st = new StringTokenizer(fullQName,".");
		Name copy = null;
		SimpleName sn = ast.newSimpleName(st.nextToken());
		while(st.hasMoreTokens()){
			SimpleName snNext = ast.newSimpleName(st.nextToken());
			if(copy == null){
			copy = ast.newQualifiedName(sn, snNext);
			}else{
				//copy = ast.newn)
			}
		}
		
		return null;
	}
	
	private Type copyTypeFromOtherASTNode(AST ast, Type type){
		if(type instanceof PrimitiveType){
			return ast.newPrimitiveType(((PrimitiveType)type).getPrimitiveTypeCode());
		}
		else if(type instanceof SimpleType){
			SimpleType simpleType = (SimpleType)type;
			
			return ast.newSimpleType(ast.newName(simpleType.getName().getFullyQualifiedName()));
		}
		else if(type instanceof ArrayType){
			ArrayType atype = (ArrayType)type;
			return ast.newArrayType(copyTypeFromOtherASTNode(ast, atype.getComponentType()));
		}
		else if(type instanceof ParameterizedType){
			ParameterizedType ptype = (ParameterizedType)type;
			ast.newParameterizedType(copyTypeFromOtherASTNode(ast, ptype.getType()));
		}
		else if(type instanceof WildcardType){
			WildcardType sourcetype = (WildcardType)type;
			WildcardType wtype = ast.newWildcardType();
			wtype.setBound(sourcetype.getBound());
			return wtype;
		}
		
		return null;
	}
	protected List<ImportDeclaration> getImportsWithoutJaxwsAnnotation(CompilationUnit cu){
		List<ImportDeclaration> importList = new ArrayList<ImportDeclaration>();
		List imports = cu.imports();
		for(Object obj: imports){
			ImportDeclaration id = (ImportDeclaration)obj;
			String imClsName = id.getName().getFullyQualifiedName();
			if(!imClsName.startsWith(PREFIX_JAXWS_ANNOTATION_CLASS)){
				importList.add(id);
			}
		}
		
		return importList;
	}
	
	private CompilationUnit getCompilationUnitForInterface(String portTypeName){
		IFile inFile = getServiceInterfaceFile(portTypeName);
		IJavaElement inSrc = JavaCore.create(inFile);
		ICompilationUnit icu = JBossWSCreationUtils.getJavaUnitFromFile(inFile);
		ASTParser astp = ASTParser.newParser(AST.JLS3);
		astp.setSource(icu);

		CompilationUnit cu = (CompilationUnit)astp.createAST(null);
		
		return cu;
	}
	
	private IFile getServiceInterfaceFile(String portTypeName){
		String packageName = model.getCustomPackage();
		IProject project = JBossWSCreationUtils.getProjectByName(model.getWebProjectName());
		IFolder pkgFolder = getPackageFolder();
		IFile inFile = pkgFolder.getFile(portTypeName + ".java");
		return inFile;
	}
	
	
	private IFolder getPackageFolder(){
		IProject project = JBossWSCreationUtils.getProjectByName(model.getWebProjectName());
		IFolder srcFolder = project.getFolder(RESOURCE_FOLDER);
		String pkgFolderName = model.getCustomPackage().replace(".", File.separator);
		return srcFolder.getFolder(pkgFolderName);
		
	}
	
	
	
	

}
