package com.github.jensim.megamanipulatior.actions.git.commit

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import java.io.File

object CommitOperator {

    fun commit(): Map<String, ApplyOutput> {
        val commitMessageKey = "Commit message"
        val result = HashMap<String, ApplyOutput>()
        DialogGenerator.askForInput(
            title = "Create commits",
            message = "Create commits for all changes in all checked out repositories",
            values = listOf(commitMessageKey),
            onOk = {
                var push = false
                val commitMessage = it[commitMessageKey]
                if (commitMessage.isNullOrEmpty()) {
                    return@askForInput
                }
                var workTitle = "Commiting"
                DialogGenerator.showConfirm("Also push?", "Also push? $commitMessage") {
                    push = true
                    workTitle += " & pushing"
                }
                val dirs = LocalRepoOperator.getLocalRepoFiles()
                dirs.mapConcurrentWithProgress(title = workTitle) { dir: File ->
                    ProcessOperator.runCommandAsync(dir, arrayOf("git", "add", "--all")).await()
                    result["commit_${dir.path}"] = ProcessOperator.runCommandAsync(dir, arrayOf("git", "commit", "-m", commitMessage)).await()

                    if (push && result["commit_${dir.path}"]?.exitCode == 0) {
                        result["push_${dir.path}"] = LocalRepoOperator.getBranch(dir)?.let { branch ->
                            ProcessOperator.runCommandAsync(dir, arrayOf("git", "push", "--set-upstream", "origin", branch)).await()
                        } ?: ApplyOutput.dummy(dir = dir.path)
                    }
                }
            },
            onCancel = {
                DialogGenerator.showConfirm("Info", "No commit performed!") {}
            }
        )
        if (result.isEmpty()) {
            result["NO RESULT"] = ApplyOutput(".", std = "", err = "", exitCode = 1)
        }
        return result
    }

    fun push(): Map<String, ApplyOutput> {
        val result = HashMap<String, ApplyOutput>()
        DialogGenerator.showConfirm("Push", "Push local commits to origin") {
            val dirs = LocalRepoOperator.getLocalRepoFiles()
            dirs.mapConcurrentWithProgress("Pushing") { dir ->
                result["push_${dir.path}"] = LocalRepoOperator.getBranch(dir)?.let { branch ->
                    ProcessOperator.runCommandAsync(dir, arrayOf("git", "push", "--set-upstream", "origin", branch)).await()
                } ?: ApplyOutput.dummy(dir = dir.path)
            }
        }
        return if (result.isEmpty()) {
            mapOf("NO RESULT" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        } else {
            result
        }
    }
}
