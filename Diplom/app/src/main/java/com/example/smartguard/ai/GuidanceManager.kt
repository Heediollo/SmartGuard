package com.example.smartguard.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class Scenario(
    val id: String,
    val trigger: String,
    val initialMessage: String,
    val steps: List<Step>
)

data class Step(
    val question: String,
    val options: List<String>,
    val responses: Map<String, Response>
)

data class Response(
    val message: String,
    val nextStep: Step?
)

object GuidanceManager {
    private const val TAG = "GuidanceManager"
    private lateinit var scenarios: Map<String, Scenario>
    private var defaultAdvice: String = "Будьте осторожны при разговоре с незнакомцами."

    fun initialize(context: Context) {
        try {
            val json = context.assets.open("guidance_scenarios.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = Gson().fromJson(json, type)

            @Suppress("UNCHECKED_CAST")
            val list = data["scenarios"] as? List<Map<String, Any>> ?: emptyList()
            scenarios = list.mapNotNull { item ->
                val id = item["id"] as? String ?: return@mapNotNull null
                val trigger = item["trigger"] as? String ?: return@mapNotNull null
                val initial = item["initial_message"] as? String ?: ""
                val steps = parseSteps(item["steps"])
                trigger to Scenario(id, trigger, initial, steps)
            }.toMap()
            defaultAdvice = data["default_advice"] as? String ?: defaultAdvice
            Log.d(TAG, "Loaded ${scenarios.size} scenarios")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
            scenarios = emptyMap()
        }
    }

    private fun parseSteps(obj: Any?): List<Step> {
        if (obj !is List<*>) return emptyList()
        return obj.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            val q = item["question"] as? String ?: return@mapNotNull null
            val opts = (item["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val resp = parseResponses(item["responses"])
            Step(q, opts, resp)
        }
    }

    private fun parseResponses(obj: Any?): Map<String, Response> {
        if (obj !is Map<*, *>) return emptyMap()
        return obj.mapNotNull { (key, value) ->
            if (value !is Map<*, *>) return@mapNotNull null
            val msg = value["message"] as? String ?: return@mapNotNull null
            val next = if (value.containsKey("options")) {
                Step(
                    question = msg,
                    options = (value["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    responses = parseResponses(value["responses"])
                )
            } else null
            key.toString() to Response(msg, next)
        }.toMap()
    }

    fun getScenario(trigger: String): Scenario? = scenarios[trigger]
    fun getDefaultAdvice(): String = defaultAdvice

    /**
     * Вычисляет следующий шаг, проходя по цепочке ответов от корня сценария.
     * @param scenario сценарий
     * @param answers список ответов пользователя (не включая самый первый выбор категории)
     * @return следующий вопрос (Step) или null, если цепочка закончилась
     */
    fun getNextStep(scenario: Scenario, answers: List<String>): Step? {
        if (scenario.steps.isEmpty()) return null

        var currentStep = scenario.steps.first()
        for (answer in answers) {
            val response = currentStep.responses[answer] ?: return null
            currentStep = response.nextStep ?: return null
        }
        return currentStep
    }
}