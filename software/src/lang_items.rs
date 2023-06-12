use core::panic::PanicInfo;

use crate::println;

#[panic_handler]
fn panic(info: &PanicInfo) -> ! {
    let err = info.message().unwrap();
    if let Some(location) = info.location() {
        println!(
            "Panicked at {}:{}, {}",
            location.file(),
            location.line(),
            err
        );
    } else {
        println!("Panicked: {}", err);
    }

    loop {}
}
