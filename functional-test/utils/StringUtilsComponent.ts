export class StringUtilsComponent {
  static formatClaimReferenceToAUIDisplayFormat(claimReference : string) {
    return claimReference.toString().replace(/\d{4}(?=.)/g, '$& ');
  }

  static getRandomNINumber() {
    const letters = "ABCEGHJKLMNPRSTWXYZ";
    const getRandomLetter = (letters: string) => letters[Math.floor(Math.random() * letters.length)];

    // Generate the NI number
    const prefix = getRandomLetter(letters) + getRandomLetter(letters);
    const digits = String(Math.floor(100000 + Math.random() * 900000));
    const suffix = getRandomLetter(letters);

    return `${prefix}${digits}${suffix}`;
  }
}

