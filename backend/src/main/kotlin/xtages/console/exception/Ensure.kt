package xtages.console.exception

import org.jooq.exception.TooManyRowsException

val ensure = Ensure()

/**
 * Constraint checker.
 */
class Ensure internal constructor() {

    /**
     * Checks that the result of calling [operation] is not `null` and return it, otherwise throws a
     * [NullValueException] with [code] and [message].
     *
     * @param operation - The lambda to run when checking for nullness.
     * @param valueDesc - The description of the value that should have not been `null`, for example: `"project.name"`.
     * @param message - A added to the [NullValueException] thrown.
     * @returns A non-null value [T].
     * @throws NullValueException if the result of [operation] is `null`.
     */
    fun <T : Any> notNull(operation: () -> T?, valueDesc: String, message: String = ""): T =
        notNull(operation = operation, valueDesc = valueDesc, lazyMessage = { message })

    /**
     * Checks that the result of calling [operation] is not `null` and return it, otherwise throws a
     * [NullValueException] with [code] and  and the result of calling [lazyMessage].
     *
     * @param operation - The lambda to run when checking for nullness.
     * @param valueDesc - The description of the value that should have not been `null`, for example: `"project.name"`.
     * @param lazyMessage - A lambda to call when [NullValueException] is thrown to generate the exception's message.
     * @returns A non-null value [T].
     * @throws NullValueException if the result of [operation] is `null`.
     */
    fun <T : Any> notNull(operation: () -> T?, valueDesc: String, lazyMessage: () -> String): T =
        notNull(value = operation(), valueDesc = valueDesc, lazyMessage = lazyMessage)

    /**
     * Checks that [value] is not `null` and return it, otherwise throws a [NullValueException] with [code] and
     * [message].
     *
     * @param value - The value to check for nullness.
     * @param valueDesc - The description of the value that should have not been `null`, for example: `"project.name"`.
     * @param message - A added to the [NullValueException] thrown.
     * @returns A non-null value [T].
     * @throws NullValueException if [value] is `null`.
     */
    fun <T : Any> notNull(value: T?, valueDesc: String, message: String = ""): T =
        notNull(value = value, valueDesc = valueDesc, lazyMessage = { message })

    /**
     * Checks that [value] is not `null` and return it, otherwise throws a [NullValueException] with [code] and the
     * result of calling [lazyMessage].
     *
     * @param value - The value to check for nullness.
     * @param valueDesc - The description of the value that should have not been `null`, for example: `"project.name"`.
     * @param lazyMessage - A lambda to call when [NullValueException] is thrown to generate the exception's message.
     * @returns A non-null value [T].
     * @throws NullValueException if [value] is `null`.
     */
    fun <T : Any> notNull(value: T?, valueDesc: String, lazyMessage: () -> String): T {
        if (value == null) {
            val message = lazyMessage()
            throw NullValueException(code = ExceptionCode.NULL_VALUE, valueDesc = valueDesc, innerMessage = message)
        }
        return value
    }

    /**
     * Checks that the result of calling [operation] is `true`, otherwise throws an [IllegalArgumentException] with
     * [code] and [message].
     *
     * @param operation - Lambda to run and compare its result to `true`.
     * @param code - The [ExceptionCode] to add to the [IllegalArgumentException] thrown when the result of [operation]
     *      is `false`.
     * @param message - A added to the [IllegalArgumentException] thrown.
     * @throws IllegalArgumentException if the result of [operation] is `false`.
     */
    fun isTrue(operation: () -> Boolean, code: ExceptionCode, message: String = "") =
        isTrue(operation = operation, code = code, lazyMessage = { message })

    /**
     * Checks that the result of calling [operation] is `true`, otherwise throws an [IllegalArgumentException] with
     * [code] and the result of calling [lazyMessage].
     *
     * @param operation - Lambda to run and compare its result to `true`.
     * @param code - The [ExceptionCode] to add to the [IllegalArgumentException] thrown when the result of [operation]
     *      is `false`.
     * @param lazyMessage - A lambda to call when [IllegalArgumentException] is thrown to generate the exception's
     *      message.
     * @throws IllegalArgumentException if the result of [operation] is `false`.
     */
    fun isTrue(operation: () -> Boolean, code: ExceptionCode, lazyMessage: () -> String) =
        isTrue(value = operation(), code = code, lazyMessage = lazyMessage)

    /**
     * Checks that [value] is `true`, otherwise throws an [IllegalArgumentException] with [code] and [message].
     *
     * @param value - The value to compare to `true`.
     * @param code - The [ExceptionCode] to add to the [IllegalArgumentException] thrown when [value] is `false`.
     * @param message - A added to the [IllegalArgumentException] thrown.
     * @throws IllegalArgumentException if [value] is `false`.
     */
    fun isTrue(value: Boolean, code: ExceptionCode, message: String = "") =
        isTrue(value = value, code = code, lazyMessage = { message })

    /**
     * Checks that [value] is `true`, otherwise throws an [IllegalArgumentException] with [code] and the result of
     * calling [lazyMessage].
     *
     * @param value - The value to compare to `true`.
     * @param code - The [ExceptionCode] to add to the [IllegalArgumentException] thrown when [value] is `false`.
     * @param lazyMessage - A lambda to call when [IllegalArgumentException] is thrown to generate the exception's
     *      message.
     * @throws IllegalArgumentException if [value] is `false`.
     */
    fun isTrue(value: Boolean, code: ExceptionCode, lazyMessage: () -> String) {
        if (!value) {
            val message = lazyMessage()
            throw IllegalArgumentException(code = code, innerMessage = message)
        }
    }

    /**
     * Checks that the result of calling [operation] is found exactly one result, otherwise throws a [NotFoundException]
     * with [code] and [message].
     *
     * @param operation - Lambda to run to get a value of type [T].
     * @param code - The [ExceptionCode] to add to the [NotFoundException] thrown when the result of [operation]
     *      either throws a [TooManyRowsException] or after calling `single` on a [Collection] it is signaled that the
     *      collection was either empty or had more than 1 element.
     * @param message - A added to the [NotFoundException] thrown.
     * @returns A non-null value [T].
     * @throws NotFoundException if the result of [operation] is not exactly one element [T].
     */
    fun <T : Any> foundOne(operation: () -> T?, code: ExceptionCode, message: String = ""): T =
        foundOne(operation = operation, code = code, lazyMessage = { message })

    /**
     * Checks that the result of calling [operation] is found exactly one result, otherwise throws a [NotFoundException]
     * with [code] and the result of calling [lazyMessage].
     *
     * @param operation - Lambda to run to get a value of type [T].
     * @param code - The [ExceptionCode] to add to the [NotFoundException] thrown when the result of [operation]
     *      either throws a [TooManyRowsException] or after calling `single` on a [Collection] it is signaled that the
     *      collection was either empty or had more than 1 element.
     * @param lazyMessage - A lambda to call when [NotFoundException] is thrown to generate the exception's
     *      message.
     * @returns A non-null value [T].
     * @throws NotFoundException if the result of [operation] is not exactly one element [T].
     */
    fun <T : Any> foundOne(operation: () -> T?, code: ExceptionCode, lazyMessage: () -> String): T {
        try {
            val obj = operation()
            if (obj == null) {
                val message = lazyMessage()
                throw NotFoundException(code = code, innerMessage = message)
            }
            return obj
        } catch (e: Exception) {
            when (e) {
                is TooManyRowsException, is NoSuchElementException, is kotlin.IllegalArgumentException -> {
                    val message = lazyMessage()
                    throw NotFoundException(code = code, innerMessage = message)
                }
                else -> throw e
            }
        }
    }
}
