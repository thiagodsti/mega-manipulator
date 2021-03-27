package com.github.jensim.megamanipulator.toolswindow

import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.apply.ApplyWindow
import com.github.jensim.megamanipulator.actions.forks.ForksWindow
import com.github.jensim.megamanipulator.actions.git.GitWindow
import com.github.jensim.megamanipulator.actions.search.SearchWindow
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWindow
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorModuleType.Companion.MODULE_TYPE_ID
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SettingsWindow
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener

object MyToolWindowFactory : ToolWindowFactory {

    private val tabs = listOf<Pair<String, ToolWindowTab>>(
        "tabTitleSettings" to SettingsWindow,
        "tabTitleSearch" to SearchWindow,
        "tabTitleApply" to ApplyWindow,
        "tabTitleClones" to GitWindow,
        "tabTitlePRsManage" to PullRequestWindow,
        "tabTitleForks" to ForksWindow,
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        tabs.sortedBy { it.second.index }.forEachIndexed { index, (headerKey, tab) ->
            if (index == 0) {
                tab.refresh()
            }
            val content1 = contentFactory.createContent(tab.content, MyBundle.message(headerKey), false)
            toolWindow.contentManager.addContent(content1)
        }
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                FilesOperator.makeUpBaseFiles()
                FilesOperator.refreshConf()
                tabs.find { it.second.index == event.index }?.second?.refresh()
                FilesOperator.makeUpBaseFiles()
            }
        })
        ProjectOperator.project = project
        FilesOperator.makeUpBaseFiles()
    }

    override fun isApplicable(project: Project): Boolean {
        return super.isApplicable(project) && isMegaManipulator(project)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return super.shouldBeAvailable(project) && isMegaManipulator(project)
    }

    private fun isMegaManipulator(project: Project): Boolean {
        val applicable = ModuleManager.getInstance(project).modules.any {
            it.moduleTypeName == MODULE_TYPE_ID
        } && super.isApplicable(project)
        if (applicable) {
            ProjectOperator.project = project
        }
        return applicable
    }
}
