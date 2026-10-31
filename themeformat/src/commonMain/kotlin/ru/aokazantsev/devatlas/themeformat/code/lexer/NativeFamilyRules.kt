package ru.aokazantsev.devatlas.themeformat.code.lexer

internal object NativeFamilyRules {

    private val cKeywords = words(
        "auto break case const continue default do else enum extern for goto if inline register restrict return sizeof " +
            "static struct switch typedef union volatile while _Alignas _Alignof _Atomic _Bool _Generic _Noreturn _Static_assert",
    )
    private val cTypes = words(
        "char double float int long short signed unsigned void bool size_t int8_t int16_t int32_t int64_t uint8_t uint16_t " +
            "uint32_t uint64_t",
    )

    val c = ProgramRules(
        keywords = cKeywords,
        types = cTypes,
        literals = words("true false NULL"),
        preprocessor = true,
        digitQuoteSeparator = true,
    )

    val cpp = ProgramRules(
        keywords = cKeywords + words(
            "alignas alignof and asm catch class concept consteval constexpr constinit const_cast co_await co_return co_yield " +
                "decltype delete dynamic_cast explicit export friend mutable namespace new noexcept not operator or private " +
                "protected public reinterpret_cast requires static_assert static_cast template this thread_local throw try typeid " +
                "typename using virtual xor",
        ),
        types = cTypes + words("wchar_t char8_t char16_t char32_t"),
        literals = words("true false nullptr NULL"),
        strings = listOf(StringRule("R\"(", ")\"", escapes = false, multiline = true), StringRule("\""), StringRule("'")),
        stringPrefixes = setOf("u8", "u", "l"),
        preprocessor = true,
        digitQuoteSeparator = true,
    )

    val objectiveC = ProgramRules(
        keywords = cKeywords + words(
            "@interface @implementation @end @protocol @property @synthesize @dynamic @class @selector @encode @try @catch " +
                "@finally @throw @autoreleasepool @optional @required @public @private @protected @import self super " +
                "nonatomic atomic strong weak copy assign readonly readwrite nullable nonnull instancetype id",
        ),
        types = cTypes + words("BOOL SEL IMP Class NSInteger NSUInteger CGFloat"),
        literals = words("YES NO nil Nil NULL true false"),
        strings = listOf(StringRule("@\"", "\""), StringRule("\""), StringRule("'")),
        annotationPrefix = '@',
        preprocessor = true,
        capitalizedTypes = true,
    )

    val csharp = ProgramRules(
        keywords = words(
            "abstract as base break case catch checked class const continue default delegate do else enum event explicit extern " +
                "finally fixed for foreach goto if implicit in interface internal is lock namespace new operator out override " +
                "params private protected public readonly record ref return sealed sizeof stackalloc static struct switch this " +
                "throw try typeof unchecked unsafe using virtual volatile while async await var dynamic get set init value when " +
                "where yield nameof",
        ),
        types = words("bool byte sbyte char decimal double float int uint long ulong short ushort object string void nint nuint"),
        strings = listOf(
            StringRule("\"\"\"", escapes = false, multiline = true),
            StringRule("$@\"", "\"", escapes = false, multiline = true),
            StringRule("@$\"", "\"", escapes = false, multiline = true),
            StringRule("@\"", "\"", escapes = false, multiline = true),
            StringRule("$\"", "\""),
            StringRule("\""),
            StringRule("'"),
        ),
        identifierExtra = "@",
        preprocessor = true,
        capitalizedTypes = true,
    )

    val swift = ProgramRules(
        keywords = words(
            "associatedtype class deinit enum extension fileprivate func import init inout internal let open operator private " +
                "precedencegroup protocol public rethrows static struct subscript typealias var break case catch continue default " +
                "defer do else fallthrough for guard if in repeat return throw switch where while as is try throws async await " +
                "actor some any self Self super convenience dynamic final lazy mutating nonmutating optional override required " +
                "unowned weak willSet didSet get set",
        ),
        literals = words("true false nil"),
        blockComment = BlockComment("/*", "*/", nested = true),
        strings = listOf(
            StringRule("\"\"\"", multiline = true, templates = StringTemplates.BACKSLASH_PAREN),
            StringRule("\"", templates = StringTemplates.BACKSLASH_PAREN),
        ),
        annotationPrefix = '@',
        hashDirectives = true,
        capitalizedTypes = true,
    )

    val go = ProgramRules(
        keywords = words(
            "break case chan const continue default defer else fallthrough for func go goto if import interface map package " +
                "range return select struct switch type var",
        ),
        types = words(
            "bool byte complex64 complex128 error float32 float64 int int8 int16 int32 int64 rune string uint uint8 uint16 " +
                "uint32 uint64 uintptr any comparable",
        ),
        literals = words("true false nil iota"),
        strings = listOf(StringRule("`", escapes = false, multiline = true), StringRule("\""), StringRule("'")),
    )

    val rust = ProgramRules(
        keywords = words(
            "as async await break const continue crate dyn else enum extern fn for if impl in let loop match mod move mut pub " +
                "ref return self Self static struct super trait type union unsafe use where while",
        ),
        types = words("bool char f32 f64 i8 i16 i32 i64 i128 isize u8 u16 u32 u64 u128 usize str"),
        literals = words("true false"),
        blockComment = BlockComment("/*", "*/", nested = true),
        strings = listOf(StringRule("\"", multiline = true)),
        stringPrefixes = setOf("b"),
        hashAttributes = true,
        macros = true,
        lifetimes = true,
        rawHashStrings = true,
        capitalizedTypes = true,
    )
}
