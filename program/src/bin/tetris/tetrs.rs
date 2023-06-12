use cpu_lib::console::set_screen_print;
use cpu_lib::monitor::monitor::clear_screen;

use crate::{
    game::{
        cycle::GameLoop,
        logic::{End, Logic},
    },
    ui::Ui,
};

pub struct Tetrs;

impl Tetrs {
    pub fn new() -> Tetrs {
        Tetrs
    }

    pub fn run(&self) -> End {
        clear_screen();

        // println!("Note that the behavior of \\xXX on byte-string literals matches the expectations established by the C program in Behavior of xXX in C; that is good. The problem is the behavior of \\xXX for XX > 0x7F in string-literal contexts, namely in the fourth and fifth examples where the .as_bytes() invocations are showing that the underlying byte array has two elements instead of one.");
        // println!("Currently, we allow any traits to be used for trait objects, but restrict the methods which can be called on such objects. Here, we propose instead restricting which traits can be used to make objects. Despite being less flexible, this will make for better error messages, less surprising software evolution, and (hopefully) better design. The motivation for the proposed change is stronger due to part of the DST changes.");
        // println!("Software evolution is improved with this proposal: imagine adding a non-object-safe method to a previously object-safe trait. With this proposal, you would then get errors wherever a trait-object is created. The error would explain why the trait object could not be created and point out exactly which method was to blame and why. Without this proposal, the only errors you would get would be where a trait object is used with a generic call and would be something like \"type error: SomeTrait does not implement SomeTrait\" - no indication that the non-object-safe method were to blame, only a failure in trait matching.");
        // println!("Note that a trait can be object-safe even if some of its methods use features that are not supported with an object receiver. This is true when code that attempted to use those features would only work if the Self type is Sized. This is why all methods that require Self:Sized are exempt from the typical rules. This is also why by-value self methods are permitted, since currently one cannot invoke pass an unsized type by-value (though we consider that a useful future extension).");
        // println!("The problem is that currently, there is no clear/consistent guideline about which of these APIs should live as methods/static functions associated with a type, and which should live in a raw submodule. Both forms appear throughout the standard library.");
        // println!("In addition, in the future box patterns are expected to be made more general by enabling them to destructure any type that implements one of the Deref family of traits. As such a generalisation may potentially lead to some currently valid programs being rejected due to the interaction with type inference or other language features, it is desirable that this particular feature stays feature gated until then.");
        // println!("This syntax simply removes the implicit Sized bound on every type parameter using the ? symbol. It resolves the problem about not mentioning Sized that the first two syntaxes didn’t. It also hints towards being related to sizedness, resolving the problem that plagued type. It also successfully states that unsizedness is only optional—that the parameter may be sized or unsized. This syntax has stuck, and is the syntax used today. Additionally, it could potentially be extended to other traits: for example, a new pointer type that cannot be dropped, &uninit, could be added, requiring that it be written to before being dropped. However, many generic functions assume that any parameter passed to them can be dropped. Drop could be made a default bound to resolve this, and Drop? would remove this bound from a type parameter.");

        // loop {}

        set_screen_print(false);

        let result = GameLoop::new(Logic::new(), Ui::default()).run();

        set_screen_print(true);
        result
    }
}
