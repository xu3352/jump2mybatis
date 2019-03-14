package com.xu3352.ideaplugin.mybatis;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从选中的文本跳转到 Mybatis SQL 配置文件
 * 按 namespace.keyword 选中的文本跳转到 Mybatis Mapper SQL文件对应的位置
 */
public class Jump2Mybatis extends AnAction {
    static Pattern PATTERN_MAPPER_NAMESPACE = Pattern.compile("<mapper\\s+namespace=\"([\\.\\w]+)\".*>");
    static String  IGNORE_STRS_REGEX        = "ServiceImpl|DaoImpl|Service|Dao";  // 默认应该为空; 最好是可以再设置里修改

    /** 主体逻辑处理 */
    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Document document = editor.getDocument();

        MapperConfig config = selectText2MapperConfig(editor, document);
        // System.out.println("mapperConfig:" + config);

        // 当前文件 和当前项目(Module)
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile currentFile = fileDocumentManager.getFile(document);
        if (currentFile != null) {
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            Module currentModule = projectFileIndex.getModuleForFile(currentFile);
            if (currentModule == null) return;

            PsiFile mapperFile = findMybatisMapperXmlFile(project, currentModule, config);
            // System.out.println("目标mapper文件:" + mapperFile);
            jump2File(project, mapperFile, config);
        }
    }

    /** 当前选中的文本 转成 MapperConfig */
    @NotNull
    private MapperConfig selectText2MapperConfig(Editor editor, Document document) {
        final SelectionModel selectionModel = editor.getSelectionModel();
        final int start = selectionModel.getSelectionStart();
        final int end = selectionModel.getSelectionEnd();
        TextRange range = new TextRange(start, end);
        String selectTxt = document.getText(range);
        // System.out.println("选中文本位置: " + start + "," + end + "   内容:" + selectTxt);

        return new MapperConfig(selectTxt);
    }

    /** 跳转到目标文件 */
    private void jump2File(Project project, PsiFile psiFile, MapperConfig mapperConfig) {
        if (psiFile == null) return;
        // 关键词:偏移量(匹配"keyword"位置)
        int offset = 0;
        if (!"".equals(mapperConfig.getKeyword())) {
            offset = psiFile.getText().indexOf("\"" + mapperConfig.getKeyword() + "\"") + 1;
        }
        // 打开文件, 并跳转到指定位置
        OpenFileDescriptor d = new OpenFileDescriptor(project, psiFile.getVirtualFile(), offset);
        d.navigate(true);
    }

    /** 找到对应的 mybatis mapper xml文件 */
    private PsiFile findMybatisMapperXmlFile(Project project, Module module, MapperConfig mapperConfig) {
        GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);
        // FilenameIndex.getFilesByName(project, "", moduleScope);

        // 选中的文本进行替换
        String selectedNamespace = mapperConfig.getNamespace().replaceAll(IGNORE_STRS_REGEX, "");

        Collection<VirtualFile> vfiles = FilenameIndex.getAllFilesByExt(project, "xml", moduleScope);
        for (VirtualFile vfile : vfiles) {
            PsiManager psiManager = PsiManager.getInstance(project);
            PsiFile file = psiManager.findFile(vfile);
            String text = file.getText();

            if (text != null) {
                // if (text.contains("<mapper namespace=\"" + namespace + "\">")) return file;
                Matcher m = PATTERN_MAPPER_NAMESPACE.matcher(text);
                if (m.find()) {
                    String namespace = m.group(1);
                    // System.out.println("find: " + namespace);

                    // 后缀匹配则返回文件
                    if (namespace.toLowerCase().endsWith(selectedNamespace.toLowerCase())) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    /** 触发条件 */
    @Override
    public void update(final AnActionEvent e) {
        //Get required data keys
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        //Set visibility only in case of existing project and editor and if some text in the editor is selected
        e.getPresentation().setVisible((project != null && editor != null && editor.getSelectionModel().hasSelection()));
    }
}
