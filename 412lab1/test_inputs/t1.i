// COMP 412, Rice University
// ILOC Front End
// 
// This file contains a couple of lexical errors
  loadI 10a => r1
  storeabc
  load  r1 => r1
// The scanner should find and report these errors.
// There is no single correct way of scanning or reporting them.
    addI r1,ra2 => rb3
