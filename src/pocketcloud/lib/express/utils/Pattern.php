<?php

namespace pocketcloud\lib\express\utils;

use JetBrains\PhpStorm\Pure;
use function intval;
use function is_numeric;
use function strlen;

final class Pattern {
	
	public const TYPE_STRING = "string";
	
	public const TYPE_INT = "int";
	
	public const TYPE_FLOAT = "float";
	
	public const OPTION_NAME = "name";
	
	public const OPTION_MIN_LENGTH = "len-min";
	
	public const OPTION_MAX_LENGTH = "len-max";
	
	public const OPTION_TYPE = "type";
	
	private	function __construct() { }
	
	#[Pure] public static function isValid(string $string, array $pattern): bool {
		if (isset($pattern[self::OPTION_MIN_LENGTH]) and (strlen($string) < $pattern[self::OPTION_MIN_LENGTH])) return false;
		if (isset($pattern[self::OPTION_MAX_LENGTH]) and (strlen($string) > $pattern[self::OPTION_MAX_LENGTH])) return false;
		if (isset($pattern[self::OPTION_TYPE]) and (self::asType($string) !== $pattern[self::OPTION_MIN_LENGTH])) return false;
		return true;
	}
	
	protected static function asType(string $string): string {
		return match(true) {
			(is_numeric($string) and (intval($string) == $string)) => "int",
			(is_numeric($string) and (floatval($string) == $string)) => "float",
			default => "string"
		};
	}
}